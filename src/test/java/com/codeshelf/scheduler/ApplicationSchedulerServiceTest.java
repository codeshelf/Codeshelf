package com.codeshelf.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.CronExpression;

import com.codeshelf.application.FacilitySchedulerService;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.ITenantManagerService;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.ScheduledJob;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.ServiceUtility;
import com.codeshelf.testframework.HibernateTest;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Service.State;

public class ApplicationSchedulerServiceTest extends HibernateTest {

	@Test
	public void persistScheduledJobs() throws Exception {
		//note that this test intended to test multitenant but couldn't get tenant session to switch back and forth effectively 
		//ListMultimap<Tenant, Facility> tenantFacilities = createTenantFacilities();
		//System.out.println(tenantFacilities.entries());
		beginTransaction();
		Facility facility1 = Facility.createFacility("F20", "", Point.getZeroPoint());
		facility1.getDao().store(facility1);
		commitTransaction();
		
		
		final ApplicationSchedulerService subject = new ApplicationSchedulerService();
		subject.startAsync();
		subject.awaitRunningOrThrow();
		
		final String testExpression = "0 0 2 * * ?";
		beginTransaction();
		ScheduledJob testJob = new ScheduledJob(facility1.reload(), ScheduledJobType.Test, testExpression);
		subject.scheduleJob(testJob);
		commitTransaction();
		
/*		
		for (final Entry<Tenant, Facility> entry : tenantFacilities.entries()) {
			new AsTenant(entry.getKey()) {
				public void doTransaction() throws Exception {
					System.out.println("all" + entry.getKey() + Facility.staticGetDao().getAll());
					ScheduledJob testJob = new ScheduledJob(entry.getValue().reload(), ScheduledJobType.Test, testExpression);
					subject.scheduleJob(testJob);
				}
			}.run();
		}*/
		subject.stopAsync();
		subject.awaitTerminatedOrThrow();

		ApplicationSchedulerService restartedSubject = new ApplicationSchedulerService();
		restartedSubject.startAsync();
		restartedSubject.awaitRunningOrThrow();
		beginTransaction();
		Optional<FacilitySchedulerService> service = restartedSubject.findService(facility1.reload());
		if (service.isPresent()) {
			Map<ScheduledJobType, CronExpression> jobs = service.get().getJobs();
			Assert.assertEquals(testExpression, jobs.get(ScheduledJobType.Test).getCronExpression());
		}
		commitTransaction();
		/*
		for (Facility facility : tenantFacilities.values()) {
			Optional<FacilitySchedulerService> service = restartedSubject.findService(facility);
			if (service.isPresent()) {
				Map<ScheduledJobType, CronExpression> jobs = service.get().getJobs();
				Assert.assertEquals(testExpression, jobs.get(ScheduledJobType.Test).getCronExpression());
			}
		}*/		
	}

	@Test
	public void updateScheduledJobs() throws Exception {
		//note that this test intended to test multitenant but couldn't get tenant session to switch back and forth effectively 
		//		ListMultimap<Tenant, Facility> tenantFacilities = createTenantFacilities();
		beginTransaction();
		Facility facility1 = Facility.createFacility("F35", "", Point.getZeroPoint());
		facility1.getDao().store(facility1);
		commitTransaction();
		
		final ApplicationSchedulerService subject = new ApplicationSchedulerService();
		subject.startAsync();
		subject.awaitRunningOrThrow();
		
		final String initialExpression = "0 0 2 * * ?";
		final String updatedExpression = "0 0 3 * * ?";

		beginTransaction();
		ScheduledJob initialJob = new ScheduledJob(facility1.reload(), ScheduledJobType.Test, initialExpression);
		subject.scheduleJob(initialJob);
		commitTransaction();

		beginTransaction();
		ScheduledJob updatedJob = new ScheduledJob(facility1.reload(), ScheduledJobType.Test, updatedExpression);
		subject.scheduleJob(updatedJob);
		commitTransaction();

		
		/*
		for (final Entry<Tenant, Facility> entry : tenantFacilities.entries()) {
			new AsTenant(entry.getKey()) {

				@Override
				protected void doTransaction() throws Exception {
					ScheduledJob initialJob = new ScheduledJob(entry.getValue().reload(), ScheduledJobType.Test, initialExpression);
					subject.scheduleJob(initialJob);
					ScheduledJob updatedJob = new ScheduledJob(entry.getValue().reload(), ScheduledJobType.Test, updatedExpression);
					subject.scheduleJob(updatedJob);
				}
				
			}.run();
		}*/

		beginTransaction();
		Optional<FacilitySchedulerService> service = subject.findService(facility1.reload());
		if (service.isPresent()) {
			Map<ScheduledJobType, CronExpression> jobs = service.get().getJobs();
			Assert.assertEquals(updatedExpression, jobs.get(ScheduledJobType.Test).getCronExpression());
		}	
		commitTransaction();

		/*
		for (final Entry<Tenant, Facility> entry : tenantFacilities.entries()) {
			new AsTenant(entry.getKey()) {

				@Override
				protected void doTransaction() throws Exception {
					Optional<FacilitySchedulerService> service = subject.findService(entry.getValue().reload());
					if (service.isPresent()) {
						Map<ScheduledJobType, CronExpression> jobs = service.get().getJobs();
						Assert.assertEquals(updatedExpression, jobs.get(ScheduledJobType.Test).getCronExpression());
					}	
				}
			}.run();
		}*/		
	}

	
	@Test
	public void startsAndStopsAllFacilitySchedulersAcrossTenants() {
		ITenantManagerService tenantManager = TenantManagerService.getInstance();
		Set<UUID> facilityIds = new HashSet<>();
		List<String> tenants = ImmutableList.of("tenant1", "tenant2");
		int count = 0;
		for (String tenantName : tenants) {
			Tenant tenant = tenantManager.createTenant(tenantName, tenantName, TenantManagerService.DEFAULT_SHARD_NAME);
			TenantPersistenceService persistence = TenantPersistenceService.getInstance();
			CodeshelfSecurityManager.removeContextIfPresent();
			CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), tenant);
			persistence.beginTransaction();
			
			Facility facility1 = Facility.createFacility("F"+count++, "", Point.getZeroPoint());
			persistence.getDao(Facility.class).store(facility1);
			facilityIds.add(facility1.getPersistentId());
			
			Facility facility2 = Facility.createFacility("F"+count++, "", Point.getZeroPoint());
			persistence.getDao(Facility.class).store(facility2);
			facilityIds.add(facility2.getPersistentId());
			
			persistence.commitTransaction();
		}
		
		ApplicationSchedulerService subject = new ApplicationSchedulerService();
		subject.startAsync();
		ServiceUtility.awaitRunningOrThrow(subject);
		
		
		Multimap<State, FacilitySchedulerService> services=  subject.getServicesByState();
		for (FacilitySchedulerService service: services.values()) {
			Assert.assertEquals(State.RUNNING, service.state()); //only once through and shoyuld be running
			UUID facilityIdToFind = service.getFacility().getPersistentId(); 
			Assert.assertTrue("Did not find service for faclitity " + service.getFacility(), facilityIds.remove(facilityIdToFind));
		}
		Assert.assertTrue("Didn't find " + facilityIds, facilityIds.isEmpty());

		subject.stopAsync();
		ServiceUtility.awaitTerminatedOrThrow(subject);
		for (String name : tenants) {
			Tenant tenant = tenantManager.getTenantByName(name);
			TenantManagerService.getInstance().deleteTenant(tenant);
		}
		CodeshelfSecurityManager.removeContextIfPresent();
	}
	
	@Test
	public void testStartsNewFacilitySchedulerForNewFacilityOfTenant() {
		
	}
	
	@Test
	public void testSchedulesDefaultedOnFacilityRecreate() {
		
	}

	@Test
	public void viewSchedulesAcrossTenantByFacility() {
		
	}

	private ListMultimap<Tenant, Facility> createTenantFacilities() throws Exception {
		final ListMultimap<Tenant, Facility> tenantFacilities = ArrayListMultimap.create();
		ITenantManagerService tenantManager = TenantManagerService.getInstance();
		List<String> tenants = ImmutableList.of("tenant1", "tenant2");
		int count = 0;
		for (String tenantName : tenants) {
			final Tenant tenant = tenantManager.createTenant(tenantName, tenantName, TenantManagerService.DEFAULT_SHARD_NAME);
			final int loopCount = count++; //hokey
			count++; //double hokey
			new AsTenant(tenant) {
				public void doTransaction() throws Exception {
					Facility facility1 = Facility.createFacility("F"+loopCount, "", Point.getZeroPoint());
					facility1.getDao().store(facility1);
					tenantFacilities.put(tenant, facility1);
					
					Facility facility2 = Facility.createFacility("F"+(loopCount+1), "", Point.getZeroPoint());
					facility2.getDao().store(facility2);
					tenantFacilities.put(tenant, facility2);
				}
			}.run();
		}
		return tenantFacilities;
	}
	
	private static abstract class AsTenant  {

		private Tenant tenant;
		
		public AsTenant(Tenant tenant) {
			this.tenant = tenant;
		}
		
		public void run() throws Exception {
			// TODO Auto-generated method stub
			TenantPersistenceService persistence = TenantPersistenceService.getInstance();
			//CodeshelfSecurityManager.removeContextIfPresent();
			CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), tenant);
			persistence.beginTransaction();
			try {
				doTransaction() ;
				persistence.commitTransaction();
			} finally {
				persistence.rollbackAnyActiveTransactions();
				CodeshelfSecurityManager.removeContextIfPresent();
			}
		}
		
		protected abstract void doTransaction() throws Exception;
	}
}
