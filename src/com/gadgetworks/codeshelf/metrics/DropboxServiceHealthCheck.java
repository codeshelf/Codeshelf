package com.gadgetworks.codeshelf.metrics;

import java.util.List;

import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.common.collect.Lists;

public class DropboxServiceHealthCheck extends CodeshelfHealthCheck {

	public DropboxServiceHealthCheck() {
		super("Dropbox service");
	}

	@Override
	protected Result check() throws Exception {
		List<Facility> failedFacilities = Lists.newArrayList();
		
		PersistenceService.getInstance().beginTenantTransaction();
		int numFacilities = -1;
		try {
			List<Facility> allFacilities = Facility.DAO.getAll();
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
			PersistenceService.getInstance().commitTenantTransaction();
		}
		
		if(failedFacilities.isEmpty()) {
			return Result.healthy("All %d facilities dropbox connections OK", numFacilities);
		}
		else {
			return Result.unhealthy("%d of %d facilities dropbox connections failed", failedFacilities.size(), numFacilities);			
		}

	}

}
