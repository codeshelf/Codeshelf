package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class NetworkStatusRequest extends RequestABC {

	@Getter @Setter
	UUID networkId;
}
