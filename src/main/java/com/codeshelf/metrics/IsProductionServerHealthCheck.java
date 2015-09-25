package com.codeshelf.metrics;

import java.util.List;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.PropertyService;

public class IsProductionServerHealthCheck extends CodeshelfHealthCheck {
	
	int productionFacilityCount = 0;
	int totalFacilityCount = 0;


	public IsProductionServerHealthCheck() {
		super("IsProduction");
	}

	@Override
	protected Result check() throws Exception {

		int totalTenantCount = 0;
		productionFacilityCount = 0;
		totalFacilityCount = 0;

		List<Tenant> tenants = TenantManagerService.getInstance().getTenants();
		totalTenantCount = tenants.size();
		for (Tenant tenant : tenants) {
			try {
				CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), tenant);
				checkFacilitiesForTenant(tenant);
			} finally {
				CodeshelfSecurityManager.removeContext();
			}
		}

		if (productionFacilityCount == 0) {
			// Not unhealthy. Just the answer to IsProduction
			return Result.unhealthy("No production sites among %d facilities in %d tenants.", totalFacilityCount, totalTenantCount);
		} else {
			return Result.healthy("Found %d production sites among %d facilities in %d tenants.",
				productionFacilityCount,
				totalFacilityCount,
				totalTenantCount);
		}
	}

	private void checkFacilitiesForTenant(Tenant tenant) {
		int totalFacilitiesThisTenant = 0;
		int productionFacilitiesThisTenant = 0;
		
		try {
			TenantPersistenceService.getInstance().beginTransaction();
			List<Facility> allFacilities = Facility.staticGetDao().getAll();
			totalFacilitiesThisTenant = allFacilities.size();
			for (Facility facility : allFacilities) {
				if (isProductionFacility(facility)){
					productionFacilitiesThisTenant++;
				}
			}
		} finally {
			TenantPersistenceService.getInstance().rollbackTransaction(); // we did not change anything
		}
		
		totalFacilityCount += totalFacilitiesThisTenant;
		productionFacilityCount += productionFacilitiesThisTenant;

	}

	private boolean isProductionFacility(Facility inFacility) {			
		IPropertyService properties = PropertyService.getInstance();
		return properties.getBooleanPropertyFromConfig(inFacility, DomainObjectProperty.PRODUCTION);
	}


}
