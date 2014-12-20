package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.List;

import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;

import lombok.Getter;
import lombok.Setter;

public class ObjectPropertiesResponse extends ResponseABC {

	@Getter @Setter
	String className;
	
	@Getter @Setter
	String persistentId;

	@Getter @Setter
	List<DomainObjectProperty> properties;

	public ObjectPropertiesResponse(String className, String persistentId) {
		this.className = className;
		this.persistentId = persistentId;
	}

}
