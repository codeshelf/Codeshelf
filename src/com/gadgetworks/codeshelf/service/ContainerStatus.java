package com.gadgetworks.codeshelf.service;

import lombok.Getter;

import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility.Work;
import com.gadgetworks.codeshelf.validation.BatchResult;

public class ContainerStatus {

	@Getter
	private Container container;
	
	@Getter
	private BatchResult<Work> result;

	public ContainerStatus(Container container, BatchResult<Work> result) {
		super();
		this.container = container;
		this.result = result;
	}
	
	
}
