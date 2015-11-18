package com.codeshelf.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.quartz.CronExpression;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.FacilitySchedulerService;
import com.codeshelf.application.FacilitySchedulerService.JobFuture;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.ITenantManagerService;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ScheduledJob;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.service.ServiceUtility;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;

public class ApplicationSchedulerService extends AbstractCodeshelfIdleService {
	static final private Logger	LOGGER						= LoggerFactory.getLogger(ApplicationSchedulerService.class);

	private ServiceManager	facilitySchedulerServiceManager;
	private JobFactory jobFactory;

	public ApplicationSchedulerService() {
		this(null);
	}
	
	@Inject
	public ApplicationSchedulerService(JobFactory jobFactory) {
		this.jobFactory = jobFactory;
	}
	
	@Override
	protected void startUp() throws Exception {
		ITenantManagerService tenantService =  TenantManagerService.getInstance();
		TenantPersistenceService persistence = TenantPersistenceService.getInstance();
		
		DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();
		List<FacilitySchedulerService> services = new ArrayList<FacilitySchedulerService>();
		for (Tenant tenant : tenantService.getTenants()) {
			UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
			CodeshelfSecurityManager.setContext(systemUser, tenant);
			persistence.beginTransaction();
			try {
				List<Facility> facilities = persistence.getDao(Facility.class).getAll();
				for (Facility facility : facilities) {
					try {
						String schedulerName = String.format("%s.%s", tenant.getTenantIdentifier(), facility.getDomainId());
						SimpleThreadPool threadPool = new SimpleThreadPool(1, Thread.MIN_PRIORITY);
						schedulerFactory.createScheduler(schedulerName, schedulerName, threadPool, new RAMJobStore());
						Scheduler facilityScheduler = schedulerFactory.getScheduler(schedulerName);
						if (jobFactory != null){
							facilityScheduler.setJobFactory(jobFactory);
						}
						FacilitySchedulerService service = new FacilitySchedulerService(facilityScheduler, systemUser, tenant, facility);
						for (ScheduledJobType jobType : ScheduledJobType.values()) {
							if (jobType.isDefaultOnOff()) {
								service.schedule(jobType.getDefaultSchedule(), jobType);
							}
						}

						
						ITypedDao<ScheduledJob> dao = persistence.getDao(ScheduledJob.class);
						List<ScheduledJob> jobs = dao.findByParent(facility);
						for (ScheduledJob job : jobs) {
							if (job.isActive()) {
								service.schedule(job.getCronExpression(), job.getType());
							}
						}
						services.add(service);
					} catch(Exception e) {
						LOGGER.error("Unable to start scheduler service for facility {}", facility, e);
					}
				}
				persistence.commitTransaction();
			} catch(Exception e) {
				persistence.rollbackAnyActiveTransactions();
				throw e;
			}
		}
		facilitySchedulerServiceManager = new ServiceManager(services);
		facilitySchedulerServiceManager.startAsync();
		ServiceUtility.awaitRunningOrThrow(facilitySchedulerServiceManager);
	}
	
	@Override
	protected void shutDown() throws Exception {
		facilitySchedulerServiceManager.stopAsync();
		ServiceUtility.awaitTerminatedOrThrow(facilitySchedulerServiceManager);
	}

	
	
	public Optional<FacilitySchedulerService> findService(Facility facility) {
		Preconditions.checkNotNull(facility, "facility cannot be null");
		for (Service  service : facilitySchedulerServiceManager.servicesByState().values()) {
			if (service instanceof FacilitySchedulerService) {
				if (((FacilitySchedulerService) service).hasFacility(facility)) {
					return Optional.of((FacilitySchedulerService) service);
				}
			}
		}
		return Optional.absent();
	}
	
	public Multimap<State, FacilitySchedulerService> getServicesByState() {
		//This method returns typed services instead of Service superclass
		Multimap<State, FacilitySchedulerService> services = ArrayListMultimap.create(); 
		for (Entry<State, Service> serviceEntry : facilitySchedulerServiceManager.servicesByState().entries()) {
			services.put(serviceEntry.getKey(), (FacilitySchedulerService) serviceEntry.getValue()); 
		}
		return ImmutableMultimap.copyOf(services);
	}

	public void scheduleJob(ScheduledJob job) throws SchedulerException {
		Optional<FacilitySchedulerService> service = findService(job.getFacility());
		if (service.isPresent()) {
			if (job.isActive()) {
				service.get().schedule(job.getCronExpression(), job.getType());
			} else {
				service.get().unschedule(job.getType());
			}
			ScheduledJob foundJob = job.getDao().findByDomainId(job.getParent(), job.getDomainId());
			if (foundJob != null) {
				foundJob.setCronExpression(job.getCronExpression());
				foundJob.setActive(job.isActive());
				job.getDao().store(foundJob);
			} else {
				job.getDao().store(job);
			}
		}
	}


	public void triggerJob(Facility facility, ScheduledJobType type) throws SchedulerException {
		Optional<FacilitySchedulerService> service = findService(facility);
		if (service.isPresent()) {
			service.get().trigger(type);
		} else {
			LOGGER.warn("Unable to trigger requested job {} for {}", type, facility);
		}
	}
	
	public Optional<CronExpression> findSchedule(Facility facility, ScheduledJobType type) throws SchedulerException {
		Optional<FacilitySchedulerService> service = findService(facility);
		if (service.isPresent()) {
			return Optional.fromNullable(service.get().getJobs().get(type));
		}
		return Optional.absent();
	}

	public boolean cancelJob(Facility facility, ScheduledJobType type) throws SchedulerException {
		Optional<FacilitySchedulerService> service = findService(facility);
		if (service.isPresent()) {
			return service.get().cancelJob(type);
		}
		return false;
	}

	public List<ScheduledJobView> getScheduledJobs(Facility facility) throws SchedulerException {
		
		
		Optional<FacilitySchedulerService> service = findService(facility);
		ArrayList<ScheduledJobView> jobViews = new ArrayList<ScheduledJobView>();
		if (service.isPresent()) {
			List<ScheduledJob> foundJobs = ScheduledJob.staticGetDao().findByParent(facility);
			for (ScheduledJob job : foundJobs) {
				Optional<JobFuture<ScheduledJobType>> future = service.get().hasRunningJob(job.getType());
				boolean running = false;
				if(future.isPresent()) {
					running = !future.get().isDone();
				}
				jobViews.add(new ScheduledJobView(job, running));
			}
			Collections.sort(jobViews, ScheduledJobView.SORT_BY_TYPE);
			return jobViews;
		}
		
		return Collections.emptyList();
	}

	public boolean removeJob(Facility facility, ScheduledJobType type) throws SchedulerException {
		Optional<FacilitySchedulerService> service = findService(facility);
		if (service.isPresent()) {
			boolean result = service.get().removeJob(type);
			ScheduledJob foundJob = ScheduledJob.staticGetDao().findByDomainId(facility, type.name());
			if (foundJob != null) {
				ScheduledJob.staticGetDao().delete(foundJob);
			}
			return result;
		}
		return false;
	}
}
