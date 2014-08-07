package com.gadgetworks.codeshelf.filter;

import java.util.Set;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;

public interface ObjectEventListener {

	String getId();

	ResponseABC processObjectAdd(IDomainObject inDomainObject);

	ResponseABC processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties);

	ResponseABC processObjectDelete(IDomainObject inDomainObject);
}
