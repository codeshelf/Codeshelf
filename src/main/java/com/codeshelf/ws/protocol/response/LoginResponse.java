package com.codeshelf.ws.protocol.response;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.manager.User;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Organization;
import com.codeshelf.model.domain.SiteController.SiteControllerRole;

public class LoginResponse extends ResponseABC {
	@Getter
	@Setter
	User				user;

	@Getter
	@Setter
	CodeshelfNetwork	network;

	@Getter
	@Setter
	Organization		organization;

	@Getter
	@Setter
	boolean				autoShortValue;

	@Getter
	@Setter
	String				pickInfoValue;

	@Getter
	@Setter
	String				containerTypeValue;

	@Getter
	@Setter
	String				scanTypeValue;

	@Getter
	@Setter
	String				sequenceKind;

	@Getter
	@Setter
	String				pickMultValue;

	@Getter
	@Setter
	String				productionValue;

	@Getter
	@Setter
	String[]			permissions;

	@Getter
	@Setter
	String				tenantName;

	@Getter
	@Setter
	String				facilityDomainId;

	@Getter
	@Setter
	String				ordersubValue;

	@Getter
	@Setter
	String				protocol;
	
	@Getter
	@Setter
	SiteControllerRole	siteControllerRole;

	public LoginResponse() {
	}
}
