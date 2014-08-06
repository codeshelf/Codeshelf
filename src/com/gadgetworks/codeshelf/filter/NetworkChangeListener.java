package com.gadgetworks.codeshelf.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkStatusResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class NetworkChangeListener implements IObjectEventListener {

	@Getter @Setter
	String id;
	
	@Getter @Setter
	private UUID networkId;

	@Override
	public ResponseABC processObjectAdd(IDomainObject inDomainObject) {
		if (inDomainObject instanceof Che || inDomainObject instanceof LedController) {
			return generateResponse();
		}
		return null;
	}

	@Override
	public ResponseABC processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		if (inDomainObject instanceof Che || inDomainObject instanceof LedController) {
			return generateResponse();
		}
		return null;
	}

	@Override
	public ResponseABC processObjectDelete(IDomainObject inDomainObject) {
		if (inDomainObject instanceof Che || inDomainObject instanceof LedController) {
			return generateResponse();
		}
		return null;
	}
	
	private NetworkStatusResponse generateResponse() {
		String filterClause = "parent.persistentId = :theId";
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("theId", this.networkId);
	
		// retrieve current list of CHEs and LED controllers
		List<Che> ches = Che.DAO.findByFilter(filterClause, filterParams);
		List<LedController> ledContollers = LedController.DAO.findByFilter(filterClause, filterParams);
	
		// create response describing network devices
		NetworkStatusResponse response = new NetworkStatusResponse();
		response.setChes(ches);
		response.setLedControllers(ledContollers);
		response.setStatus(ResponseStatus.Success);	
		return response;
	}
}
