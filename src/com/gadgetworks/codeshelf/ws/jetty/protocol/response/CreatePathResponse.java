package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.Path;

public class CreatePathResponse extends ResponseABC {
	@Getter @Setter
	Path path;
}
