package com.gadgetworks.codeshelf.filter;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;

import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.model.domain.WirelessDeviceABC;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.NetworkStatusMessage;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class NetworkChangeListener implements ObjectEventListener {

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
	public MessageABC processObjectDelete(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		return onAnythingChanged(domainClass, domainPersistentId);
	}
	
	private MessageABC onAnythingChanged(Class<? extends IDomainObject> domainClass, final UUID domainPersistentId) {
		CodeshelfNetwork network = null;
		if(WirelessDeviceABC.class.isAssignableFrom(domainClass)) {
			WirelessDeviceABC object = (WirelessDeviceABC) PersistenceService.getDao(domainClass).findByPersistentId(domainClass, domainPersistentId);
			network = object.getParent();
			
		} else if(CodeshelfNetwork.class.isAssignableFrom(domainClass)) {
			network = (CodeshelfNetwork) PersistenceService.getDao(domainClass).findByPersistentId(domainClass, domainPersistentId);
		}
		if(network != null) {
			// if the object changed within this network, generate a new network status response
			if(network.equals(this.network)) {
				return new NetworkStatusMessage(PersistenceService.<CodeshelfNetwork>deproxify(network));
			}
		}
		return null;
	}
	
	public static void registerWithSession(ObjectChangeBroadcaster objectChangeBroadcaster, UserSession session, CodeshelfNetwork network) {
		// register network change listener
		NetworkChangeListener listener = new NetworkChangeListener(network,"network-change-listener");
		session.registerObjectEventListener(listener);
		// register DAOs
		objectChangeBroadcaster.registerDAOListener(session, Che.class);
		objectChangeBroadcaster.registerDAOListener(session, LedController.class);
		objectChangeBroadcaster.registerDAOListener(session, SiteController.class);
		objectChangeBroadcaster.registerDAOListener(session, CodeshelfNetwork.class);
	}
}
