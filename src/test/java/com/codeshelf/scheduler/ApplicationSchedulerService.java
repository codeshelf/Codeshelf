package com.codeshelf.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mockito.asm.tree.MultiANewArrayInsnNode;
import org.quartz.Scheduler;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.FacilitySchedulerService;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.ITenantManagerService;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.codeshelf.service.ServiceUtility;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

public class ApplicationSchedulerService extends AbstractCodeshelfIdleService {
	static final private Logger	LOGGER						= LoggerFactory.getLogger(ApplicationSchedulerService.class);

	private ServiceManager	facilitySchedulerServiceManager;

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
						FacilitySchedulerService service = new FacilitySchedulerService(facilityScheduler, systemUser, tenant, facility);
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

	public Multimap<State, FacilitySchedulerService> getServicesByState() {
		Multimap<State, FacilitySchedulerService> services = ArrayListMultimap.create(); 
		for (Entry<State, Service> serviceEntry : facilitySchedulerServiceManager.servicesByState().entries()) {
			services.put(serviceEntry.getKey(), (FacilitySchedulerService) serviceEntry.getValue()); 
		}
		return ImmutableMultimap.copyOf(services);
	}

}
