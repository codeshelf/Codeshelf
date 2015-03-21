package com.codeshelf.metrics;

import java.util.List;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.model.domain.DropboxService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.google.common.collect.Lists;

public class DropboxServiceHealthCheck extends CodeshelfHealthCheck {

	public DropboxServiceHealthCheck() {
		super("Dropbox service");
	}

	@Override
	protected Result check() throws Exception {
		
		// TODO: what do we check on multitenant server?
		Tenant tenant = TenantManagerService.getInstance().getInitialTenant();		
		
		List<Facility> failedFacilities = Lists.newArrayList();
		
		TenantPersistenceService.getInstance().beginTransaction(tenant);
		int numFacilities = -1;
		try {
			List<Facility> allFacilities = Facility.staticGetDao().getAll(tenant);
			for (Facility facility : allFacilities) {
				DropboxService service = facility.getDropboxService();
				if (service != null) {
					if (service.checkConnectivity() == false) {
						failedFacilities.add(facility);
					}
				} 
			}
			numFacilities = allFacilities.size();
		} finally {
			TenantPersistenceService.getInstance().commitTransaction(tenant);
		}
		
		if(failedFacilities.isEmpty()) {
			return Result.healthy("All %d facilities dropbox connections OK", numFacilities);
		}
		else {
			return Result.unhealthy("%d of %d facilities dropbox connections failed", failedFacilities.size(), numFacilities);			
		}

	}

}
