package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.LedController;

public class NetworkStatusResponse extends ResponseABC {

	@Getter  @Setter
	List<Che> ches;
	
	@Getter  @Setter
	List<LedController> ledControllers;
}
