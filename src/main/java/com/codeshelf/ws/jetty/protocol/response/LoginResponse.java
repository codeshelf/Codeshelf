package com.codeshelf.ws.jetty.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Organization;
import com.codeshelf.platform.multitenancy.User;

public class LoginResponse extends ResponseABC {
	@Getter @Setter
	User user;
	
	@Getter @Setter
	CodeshelfNetwork network;
	
	@Getter @Setter
	Organization organization;
	
	@Getter @Setter
	boolean autoShortValue;

	@Getter
	@Setter
	String					pickInfoValue;

	@Getter
	@Setter
	String					containerTypeValue;
	
	@Getter
	@Setter
	String					scanTypeValue;


	public LoginResponse() {
	}
}
