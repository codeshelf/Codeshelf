package com.codeshelf.ws.protocol.response;

import java.util.List;

import com.codeshelf.model.domain.FacilityProperty;

import lombok.Getter;
import lombok.Setter;

public class ObjectPropertiesResponseNew extends ResponseABC {
	@Getter @Setter
	private List<FacilityProperty> results;
}