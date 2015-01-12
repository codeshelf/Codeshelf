package com.gadgetworks.codeshelf.metrics;

import java.util.List;

import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.google.common.collect.Lists;

public class DropboxServiceHealthCheck extends CodeshelfHealthCheck {

	public DropboxServiceHealthCheck() {
		super("Dropbox service");
	}

	@Override
	protected Result check() throws Exception {
		List<Facility> failedFacilities = Lists.newArrayList();
		List<Facility> allFacilities = Facility.DAO.getAll();
		for (Facility facility : allFacilities) {
			DropboxService service = facility.getDropboxService();
			if (service != null) {
				if (service.checkConnectivity() == false) {
					failedFacilities.add(facility);
				}
			} 
		}
		
		if(failedFacilities.isEmpty()) {
			return Result.healthy("All %d facilities dropbox connections OK", allFacilities.size());
		}
		else {
			return Result.unhealthy("%d of %d facilities dropbox connections failed", failedFacilities.size(), allFacilities.size());			
		}

	}

}
