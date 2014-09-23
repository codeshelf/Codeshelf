package com.gadgetworks.codeshelf.filter;

import java.util.Set;

import lombok.Getter;

import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.model.domain.WirelessDeviceABC;
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
	public MessageABC processObjectAdd(IDomainObject inDomainObject) {
		return onAnythingChanged(inDomainObject);
	}

	@Override
	public MessageABC processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		return onAnythingChanged(inDomainObject);
	}

	@Override
	public MessageABC processObjectDelete(IDomainObject inDomainObject) {
		return onAnythingChanged(inDomainObject);
	}
	
	private MessageABC onAnythingChanged(IDomainObject inDomainObject) {
		CodeshelfNetwork network = null;
		if(inDomainObject instanceof WirelessDeviceABC) {
			WirelessDeviceABC device = (WirelessDeviceABC)inDomainObject;
			network = device.getParent();
			
		} else if(inDomainObject instanceof CodeshelfNetwork) {
			network = (CodeshelfNetwork) inDomainObject;
		}
		if(network != null) {
			// if the object changed within this network, generate a new network status response
			if(network.equals(this.network)) {
				return new NetworkStatusMessage(network);
			}
		}
		return null;
	}
	
	public static void registerWithSession(UserSession session, CodeshelfNetwork network, IDaoProvider daoProvider) {
		// register network change listener
		NetworkChangeListener listener = new NetworkChangeListener(network,"network-change-listener");
		session.registerObjectEventListener(listener);

		// register session with daos
		Class<?> cheClass = Che.class;
		@SuppressWarnings("unchecked")
		ITypedDao<IDomainObject> cheDao = daoProvider.getDaoInstance((Class<IDomainObject>) cheClass);
		session.registerAsDAOListener(cheDao);
		
		Class<?> ledControllerClass = LedController.class;
		@SuppressWarnings("unchecked")
		ITypedDao<IDomainObject> ledControllerDao = daoProvider.getDaoInstance((Class<IDomainObject>) ledControllerClass);
		session.registerAsDAOListener(ledControllerDao);

		Class<?> siteControllerClass = SiteController.class;
		@SuppressWarnings("unchecked")
		ITypedDao<IDomainObject> siteControllerDao = daoProvider.getDaoInstance((Class<IDomainObject>) siteControllerClass);
		session.registerAsDAOListener(siteControllerDao);	}
	
}
