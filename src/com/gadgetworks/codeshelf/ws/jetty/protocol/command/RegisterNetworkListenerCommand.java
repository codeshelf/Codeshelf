package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.NetworkChangeListener;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkStatusRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkStatusResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class RegisterNetworkListenerCommand extends CommandABC {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(RegisterNetworkListenerCommand.class);

	NetworkStatusRequest request;

	public RegisterNetworkListenerCommand(CsSession session, NetworkStatusRequest request) {
		super(session);
		this.request = request;
	}

	@Override
	public ResponseABC exec() {
		UUID networkId = request.getNetworkId();
		String filterClause = "parent.persistentId = :theId";
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("theId", networkId);

		// retrieve current list of CHEs and LED controllers
		List<Che> ches = Che.DAO.findByFilter(filterClause, filterParams);
		List<LedController> ledContollers = LedController.DAO.findByFilter(filterClause, filterParams);

		// register network change listener
		NetworkChangeListener listener = new NetworkChangeListener();
		listener.setNetworkId(networkId);
		listener.setId("network-change-listener");
		session.registerObjectEventListener(listener);

		// register session with daos
		Class<?> cheClass = Che.class;
		ITypedDao<IDomainObject> cheDao = daoProvider.getDaoInstance((Class<IDomainObject>) cheClass);
		session.registerAsDAOListener(cheDao);
		Class<?> ledControllerClass = LedController.class;
		ITypedDao<IDomainObject> ledControllerDao = daoProvider.getDaoInstance((Class<IDomainObject>) ledControllerClass);
		session.registerAsDAOListener(ledControllerDao);

		// create response describing network devices
		NetworkStatusResponse response = new NetworkStatusResponse();
		response.setChes(ches);
		response.setLedControllers(ledContollers);
		response.setStatus(ResponseStatus.Success);

		return response;
	}
}
