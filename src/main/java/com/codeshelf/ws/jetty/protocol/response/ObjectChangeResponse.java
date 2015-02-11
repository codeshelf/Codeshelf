package com.codeshelf.ws.jetty.protocol.response;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class ObjectChangeResponse extends ResponseABC {
	@Getter @Setter
	List<Map<String, Object>> results;
}
