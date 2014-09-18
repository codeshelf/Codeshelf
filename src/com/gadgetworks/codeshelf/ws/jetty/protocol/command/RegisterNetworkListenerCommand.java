package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.NetworkChangeListener;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkStatusRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkStatusResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class RegisterNetworkListenerCommand extends CommandABC {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER = LoggerFactory.getLogger(RegisterNetworkListenerCommand.class);

	NetworkStatusRequest request;

	public RegisterNetworkListenerCommand(CsSession session, NetworkStatusRequest request) {
		super(session);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		UUID networkId = request.getNetworkId();
		
		// register network change listener
		NetworkChangeListener listener = new NetworkChangeListener(networkId,"network-change-listener");
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
		session.registerAsDAOListener(siteControllerDao);

		// create response describing network devices
		return NetworkStatusResponse.generate(networkId);
	}
}
