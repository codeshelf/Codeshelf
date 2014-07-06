package com.gadgetworks.codeshelf.ws.jetty.response;

import com.gadgetworks.codeshelf.model.domain.Organization;

import lombok.Getter;
import lombok.Setter;

public class LoginResponse extends ResponseABC {

	@Getter @Setter
	private Organization organization;
	
	public LoginResponse() {
	}
}
