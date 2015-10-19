package com.codeshelf.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.application.FacilitySchedulerService;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.ITenantManagerService;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.ServiceUtility;
import com.codeshelf.testframework.HibernateTest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Service.State;

public class ApplicationSchedulerServiceTest extends HibernateTest {

	
	
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
	
}
