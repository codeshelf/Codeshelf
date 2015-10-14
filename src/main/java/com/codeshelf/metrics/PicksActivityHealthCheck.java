package com.codeshelf.metrics;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.DomainObjectManager;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;

public class PicksActivityHealthCheck extends CodeshelfHealthCheck {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(PicksActivityHealthCheck.class);

	int							picksLastOneHour	= 0;
	int							picksLast24Hours	= 0;

	public PicksActivityHealthCheck() {
		super("PicksActivity");
	}

	@Override
	protected Result check() throws Exception {

		picksLastOneHour	= 0;
		picksLast24Hours	= 0;
		
		List<Tenant> tenants = TenantManagerService.getInstance().getTenants();
		for (Tenant tenant : tenants) {
			try {
				CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), tenant);
				checkTenantActivity(tenant);
			} finally {
				CodeshelfSecurityManager.removeContext();
			}
		}
		return Result.healthy("WorkInstructions completed last 1 hr:  %d; last 24 hrs: %d.", picksLastOneHour, picksLast24Hours);
	}

	private void checkTenantActivity(Tenant tenant) {

		try {
			TenantPersistenceService.getInstance().beginTransaction();
			List<Facility> allFacilities = Facility.staticGetDao().getAll();
			for (Facility facility : allFacilities) {
				checkFacilityActivity(facility);
			}
		} finally {
			TenantPersistenceService.getInstance().rollbackTransaction(); // we did not change anything
		}
	}

	private void checkFacilityActivity(Facility inFacility) {
		try {
			DomainObjectManager doMananager = new DomainObjectManager(inFacility);
			DomainObjectManager.FacilityPickParameters pickParms = doMananager.getFacilityPickParameters();
			picksLastOneHour += pickParms.getPicksLastOneHour();
			picksLast24Hours += pickParms.getPicksLastTwentyFourHours();
		} catch (Exception e) {
			LOGGER.error("checkFacilityActivity", e);
		}
	}

}
