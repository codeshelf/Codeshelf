package com.codeshelf.filter;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.model.domain.WirelessDeviceABC;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.protocol.message.NetworkStatusMessage;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class NetworkChangeListener implements ObjectEventListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkChangeListener.class);

	@Getter
	String id;
	
	@Getter
	private CodeshelfNetwork network;

	private NetworkChangeListener(CodeshelfNetwork network,String listenerId) {
		this.network=network;
		this.id=listenerId;
	}
	
	@Override
	public MessageABC processObjectAdd(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		return onAnythingChanged(domainClass, domainPersistentId);
	}

	@Override
	public MessageABC processObjectUpdate(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, Set<String> inChangedProperties) {
		return onAnythingChanged(domainClass, domainPersistentId);
	}

	@Override
	public MessageABC processObjectDelete(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId, Class<? extends IDomainObject> parentClass, final UUID parentPersistentId) {
		MessageABC result = null;
		if(parentClass == null) {
			LOGGER.error("don't know how to handle network change event with no parent");
		} else if(!CodeshelfNetwork.class.isAssignableFrom(parentClass)) {
			LOGGER.error("don't know how to handle network change event where network is not the parent");
		} else {
			// fire change event on the network
			result = onAnythingChanged(parentClass, parentPersistentId); 
		}		
		return result; 
	}
	
	private MessageABC onAnythingChanged(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		CodeshelfNetwork network = null;
		if(WirelessDeviceABC.class.isAssignableFrom(domainClass)) {
			WirelessDeviceABC object = (WirelessDeviceABC) TenantPersistenceService.getInstance().getDao(domainClass).findByPersistentId(domainPersistentId);
			network = object.getParent();
			
		} else if(CodeshelfNetwork.class.isAssignableFrom(domainClass)) {
			network = (CodeshelfNetwork) TenantPersistenceService.getInstance().getDao(domainClass).findByPersistentId(domainPersistentId);
		}
		if(network != null) {
			// if the object changed within this network, generate a new network status response
			if(network.equals(this.network)) {
				return new NetworkStatusMessage(TenantPersistenceService.<CodeshelfNetwork>deproxify(network));
			}
		}
		return null;
	}
	
	public static void registerWithSession(ObjectChangeBroadcaster objectChangeBroadcaster, WebSocketConnection session, CodeshelfNetwork network) {
		// register network change listener
		NetworkChangeListener listener = new NetworkChangeListener(network,"network-change-listener");
		session.registerObjectEventListener(listener);
		String tenantIdentifier = session.getUser().getTenant().getSchemaName();
		// register DAOs
		objectChangeBroadcaster.registerDAOListener(tenantIdentifier,session, Che.class);
		objectChangeBroadcaster.registerDAOListener(tenantIdentifier,session, LedController.class);
		objectChangeBroadcaster.registerDAOListener(tenantIdentifier,session, SiteController.class);
		objectChangeBroadcaster.registerDAOListener(tenantIdentifier,session, CodeshelfNetwork.class);
	}
}
