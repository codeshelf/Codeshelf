package com.codeshelf.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;

public class ApplicationSchedulerService extends AbstractCodeshelfIdleService {
	static final private Logger	LOGGER	= LoggerFactory.getLogger(ApplicationSchedulerService.class);

	private List<FacilitySchedulerService> services;
	private JobFactory jobFactory;

	public ApplicationSchedulerService() {
		this(null);
	}
	
	@Inject
	public ApplicationSchedulerService(JobFactory jobFactory) {
		this.jobFactory = jobFactory;
		this.services = new ArrayList<FacilitySchedulerService>();
	}
	
	@Override
	protected void startUp() throws Exception {
		ITenantManagerService tenantService =  TenantManagerService.getInstance();
		TenantPersistenceService persistence = TenantPersistenceService.getInstance();
		
		for (Tenant tenant : tenantService.getTenants()) {
			UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
			CodeshelfSecurityManager.setContext(systemUser, tenant);
			persistence.beginTransaction();
			try {
				List<Facility> facilities = persistence.getDao(Facility.class).getAll();
				for (Facility facility : facilities) {
					try {
						startFacility(tenant, facility);
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
		ServiceUtility.awaitRunningOrThrow(services);
	}
	

	@Override
	protected void shutDown() throws Exception {
		ServiceUtility.stopAsync(services);
		ServiceUtility.awaitTerminatedOrThrow(services);
	}

	
	
	public Optional<FacilitySchedulerService> findService(Facility facility) {
		Preconditions.checkNotNull(facility, "facility cannot be null");
		for (FacilitySchedulerService  service : services) {
			if (service.hasFacility(facility)) {
				return Optional.of((FacilitySchedulerService) service);
			}
		}
		return Optional.absent();
	}
	
	public Multimap<State, FacilitySchedulerService> getServicesByState() {
		//This method returns typed services instead of Service superclass
		Multimap<State, FacilitySchedulerService> servicesByState = ArrayListMultimap.create(); 
		for (Service  service : services) {
			servicesByState.put(service.state(), (FacilitySchedulerService) service); 
		}
		return ImmutableMultimap.copyOf(servicesByState);
	}

	public void updateScheduledJob(ScheduledJob job) throws SchedulerException {
		Facility facility = job.getFacility();
		Optional<FacilitySchedulerService> service = findService(facility);
		if (service.isPresent()) {
			syncByConfig(service.get(), job);
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
		Map<ScheduledJobType, ScheduledJobView> jobViews = new HashMap<ScheduledJobType, ScheduledJobView>();
		if (service.isPresent()) {
			Map<ScheduledJobType, CronExpression> jobs = service.get().getJobs();
			for (ScheduledJobType type : EnumSet.allOf(ScheduledJobType.class)) {
				boolean running = false;
				if (jobs.containsKey(type)) {
					Optional<JobFuture<ScheduledJobType>> future = service.get().hasRunningJob(type);
					if(future.isPresent()) {
						running = !future.get().isDone();
					}
				}

				ScheduledJob foundJob = findByType(facility, type);
				if (foundJob != null) {
					jobViews.put(type, new ScheduledJobView(foundJob, running));
				}
				else if (type.isDefaultOnOff()){
					jobViews.put(type, new ScheduledJobView(type, facility.getTimeZone(), running));
				}
			}
			ArrayList<ScheduledJobView> viewCollection = new ArrayList<>(jobViews.values());
			Collections.sort(viewCollection, ScheduledJobView.SORT_BY_TYPE);
			return viewCollection;
		}
		
		return Collections.emptyList();
	}

	public boolean removeJob(Facility facility, ScheduledJobType type) throws SchedulerException {
		Optional<FacilitySchedulerService> service = findService(facility);
		if (service.isPresent()) {
			boolean result = service.get().removeJob(type);
			ScheduledJob foundJob = findByType(facility, type);
			if (foundJob != null) {
				ScheduledJob.staticGetDao().delete(foundJob);
			}
			syncByJobType(service.get(), type);
			return result;
		}
		return false;
	}
	
	private ScheduledJob findByType(Facility facility, ScheduledJobType type) {
		ScheduledJob foundJob = ScheduledJob.staticGetDao().findByDomainId(facility, type.name());
		return foundJob;
	}

	private void syncByConfig(FacilitySchedulerService service, ScheduledJob job) throws SchedulerException {
		//Always schedule the job with the scheduler, but begin paused if inactive
		ScheduledJobType type = job.getType();
		service.schedule(job.getCronExpression(), type);
		if (job.isActive()) {
			service.resumeJob(type); //make sure resumed if the state has changed
			//to prevent "catch up execution" when resuming, the scheduler jobstore has low misfire threshold 
			// and trigger is set to not act when misfired
		} else {
			service.pauseJob(type);
			AbstractFacilityJob.disabled(service.getFacility(), type.getJobClass());
		}
		
	}
	
	private void syncByJobType(FacilitySchedulerService service, ScheduledJobType jobType) throws SchedulerException {
		if (jobType.isDefaultOnOff()) {
			service.schedule(jobType.getDefaultSchedule(service.getFacility().getTimeZone()), jobType);
		} else {
			AbstractFacilityJob.disabled(service.getFacility(), jobType.getJobClass());
		}
	}

	public FacilitySchedulerService startFacility(Tenant tenant, Facility facility) throws SchedulerException {
		String schedulerName = String.format("%s.%s.%s", tenant.getTenantIdentifier(), facility.getDomainId(), facility.getPersistentId());
		SimpleThreadPool threadPool = new SimpleThreadPool(2, Thread.MIN_PRIORITY); //Allow at least two simultaneous tasks per facility
		DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();
		RAMJobStore store = new RAMJobStore();
		store.setMisfireThreshold(5000); //basically if paused or delayed don't try to catch up  
		schedulerFactory.createScheduler(schedulerName, schedulerName, threadPool, store);
		Scheduler facilityScheduler = schedulerFactory.getScheduler(schedulerName);
		if (jobFactory != null){
			facilityScheduler.setJobFactory(jobFactory);
		}
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		FacilitySchedulerService service = new FacilitySchedulerService(facilityScheduler, systemUser, tenant, facility);
		for (ScheduledJobType jobType : ScheduledJobType.values()) {
			ITypedDao<ScheduledJob> dao = ScheduledJob.staticGetDao();
			List<ScheduledJob> jobs = dao.findByFilter(ImmutableList.of(Restrictions.eq("parent", facility),
																		Restrictions.eq("type", jobType)));
			if (jobs.isEmpty()) {
				syncByJobType(service, jobType);
			} else {
				if (jobs.size() > 1) {
					LOGGER.warn("should not have more than one scheduled job for {} in facility {}", jobType, facility);
				}
				ScheduledJob job = jobs.get(0);
				syncByConfig(service, job);
			}
		}
		service.startAsync();
		ServiceUtility.awaitRunningOrThrow(service);
		services.add(service);
		return service;
	}

	public void stopFacility(Facility facility) {
		Optional<FacilitySchedulerService> service = findService(facility);
		if (service.isPresent()) {
			service.get().stopAsync();
			ServiceUtility.awaitTerminatedOrThrow(service.get());
			services.remove(service);
		}
	}

}
