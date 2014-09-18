package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;

public class LoginResponse extends ResponseABC {

	@Getter @Setter
	private Organization organization;
	
	@Getter @Setter
	User user;

	@Getter @Setter
	UUID networkId;
	
	public LoginResponse() {
	}
}
