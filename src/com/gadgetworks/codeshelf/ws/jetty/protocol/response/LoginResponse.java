package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;

public class LoginResponse extends ResponseABC {
	
	@Getter @Setter
	User user;
	
	@Getter @Setter
	CodeshelfNetwork network;

	public LoginResponse() {
	}
}
