package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.SiteController;

public class NetworkStatusResponse extends ResponseABC {

	@Getter @Setter
	List<Che> ches;
	
	@Getter @Setter
	List<LedController> ledControllers;

	@Getter @Setter
	List<SiteController> siteControllers;
	
	public NetworkStatusResponse() {	
	}
	
	public NetworkStatusResponse(List<Che> ches,List<LedController> ledControllers,List<SiteController> siteControllers) {
		this.ches=ches;
		this.ledControllers=ledControllers;
		this.siteControllers=siteControllers;
		this.setStatus(ResponseStatus.Success);
	}
	
	public static ResponseABC generate(UUID networkId) {
		String filterClause = "parent.persistentId = :theId";
		Map<String, Object> filterParams = new HashMap<String, Object>();
		filterParams.put("theId", networkId);
	
		// retrieve current list of CHEs and LED controllers
		List<Che> cheList = Che.DAO.findByFilter(filterClause, filterParams);
		List<LedController> ledControllerList = LedController.DAO.findByFilter(filterClause, filterParams);
		List<SiteController> siteControllerList = SiteController.DAO.findByFilter(filterClause, filterParams);
	
		// create response describing network devices
		return new NetworkStatusResponse(cheList,ledControllerList,siteControllerList);
	}
}
