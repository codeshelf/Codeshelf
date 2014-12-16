package com.gadgetworks.codeshelf.application.apiresources;

import java.io.Serializable;
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
	
	public static class UUIDParam implements Serializable{
		private String raw;
		private UUID uuid; 
				
		public UUIDParam(String str) {
			raw = str;
			try {
				uuid = UUID.fromString(str);
			} catch (Exception e) {
				
			}
		}

		public String getRawValue(){
			return raw;
		}

		public UUID getUUID(){
			return uuid;
		}
	}
}
