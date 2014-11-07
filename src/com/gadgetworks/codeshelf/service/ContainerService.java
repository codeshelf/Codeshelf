package com.gadgetworks.codeshelf.service;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Facility.Work;
import com.gadgetworks.codeshelf.validation.BatchResult;
import com.google.common.collect.Lists;

public class ContainerService implements IApiService {

	public List<ContainerStatus> containersWithViolations(Facility facility) {
		if(facility == null) {
			throw new NullPointerException("null Facility in containersWithViolations");
		}
		List<ContainerStatus> containerStatuses = Lists.newArrayList();
		List<Container> containers = facility.getContainers();
		for (Container container : containers) {
			BatchResult<Work> workResults = facility.determineWorkForContainer(container);
			//if (!workResults.isSuccessful()) {
			containerStatuses.add(new ContainerStatus(container, workResults));
			//}
		}
		return containerStatuses;
	}
	

}
