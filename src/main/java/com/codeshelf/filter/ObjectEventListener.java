package com.gadgetworks.codeshelf.filter;

import java.util.Set;
import java.util.UUID;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public interface ObjectEventListener {

	String getId();

	MessageABC processObjectAdd(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId);

	MessageABC processObjectUpdate(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, Set<String> inChangedProperties);

	MessageABC processObjectDelete(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId);
}
