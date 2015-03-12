package com.codeshelf.filter;

import java.util.Set;
import java.util.UUID;

import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;

public interface ObjectEventListener {

	String getId();

	MessageABC processObjectAdd(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId);

	MessageABC processObjectUpdate(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, Set<String> inChangedProperties);

	MessageABC processObjectDelete(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, Class<? extends IDomainObject> parentClass, final UUID parentId);
}
