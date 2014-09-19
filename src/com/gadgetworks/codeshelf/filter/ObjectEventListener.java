package com.gadgetworks.codeshelf.filter;

import java.util.Set;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public interface ObjectEventListener {

	String getId();

	MessageABC processObjectAdd(IDomainObject inDomainObject);

	MessageABC processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties);

	MessageABC processObjectDelete(IDomainObject inDomainObject);
}
