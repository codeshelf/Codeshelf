package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class RegisterFilterResponse extends ResponseABC {

	@Getter @Setter
	List<Map<String, Object>> results;

}
