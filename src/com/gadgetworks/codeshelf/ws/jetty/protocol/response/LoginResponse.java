package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.platform.multitenancy.User;

public class LoginResponse extends ResponseABC {
	@Getter @Setter
	User user;
	
	@Getter @Setter
	CodeshelfNetwork network;
	
	@Getter @Setter
	boolean autoShortValue;

	@Getter
	@Setter
	String					pickInfoValue;

	@Getter
	@Setter
	String					containerTypeValue;


	public LoginResponse() {
	}
}
