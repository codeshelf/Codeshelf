package com.gadgetworks.codeshelf.application.apiresources;

import java.util.UUID;

import javax.ws.rs.core.Response;

import lombok.Setter;

public class BaseResponse {
	@Setter
	private int status = 200;
		
	public static UUID parseUUID(String str) {
		try {
			return UUID.fromString(str);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public Response buildResponse(){
		return Response.status(status).entity(this).build();
	}
}
