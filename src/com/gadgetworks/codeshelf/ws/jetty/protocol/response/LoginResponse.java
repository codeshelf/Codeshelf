package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.Organization;

public class LoginResponse extends ResponseABC {

	@Getter @Setter
	private Organization organization;
	
	public LoginResponse() {
	}
}
