package com.codeshelf.metrics;

import java.util.List;

import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.DropboxService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.google.common.collect.Lists;

public class DropboxServiceHealthCheck extends CodeshelfHealthCheck {

	public DropboxServiceHealthCheck() {
		super("Dropbox service");
	}

	@Override
	protected Result check() throws Exception {
		List<Facility> failedFacilities = Lists.newArrayList();
		
		// checks initial tenant only
		CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), 
			TenantManagerService.getInstance().getInitialTenant());
		
		int numFacilities = -1;
		try {
			TenantPersistenceService.getInstance().beginTransaction();
			List<Facility> allFacilities = Facility.staticGetDao().getAll();
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
			TenantPersistenceService.getInstance().rollbackTransaction();
			CodeshelfSecurityManager.removeContext();
		}
		
		if(failedFacilities.isEmpty()) {
			return Result.healthy("All %d facilities dropbox connections OK", numFacilities);
		}
		else {
			return Result.unhealthy("%d of %d facilities dropbox connections failed", failedFacilities.size(), numFacilities);			
		}

	}

}
