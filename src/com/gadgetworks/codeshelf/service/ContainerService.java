package com.gadgetworks.codeshelf.service;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Facility.Work;
import com.gadgetworks.codeshelf.validation.BatchResult;
import com.google.common.collect.Maps;

public class ContainerService implements IApiService {

	public Map<Container, BatchResult<Work>> containersWithViolations(String facilityPersistentId) {
		Facility facility = checkFacility(facilityPersistentId);
		Map<Container, BatchResult<Work>> results = Maps.newHashMap();
		List<Container> containers = facility.getContainers();
		for (Container container : containers) {
			BatchResult<Work> workResults = facility.determineWorkForContainer(container);
			//if (!workResults.isSuccessful()) {
				results.put(container, workResults);
			//}
		}
		return results;
	}
	
	private Facility checkFacility(final String facilityPersistentId) {
		return checkNotNull(Facility.DAO.findByPersistentId(facilityPersistentId), "Unknown facility: %s", facilityPersistentId);
	}


}
