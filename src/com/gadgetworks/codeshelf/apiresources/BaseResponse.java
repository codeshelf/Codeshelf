package com.gadgetworks.codeshelf.apiresources;

import java.io.Serializable;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

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
		return buildResponse(this, status);
	}

	public static Response buildResponse(int status) {
		return buildResponse(null, status);
	}

	public static Response buildResponse(Object obj, int status) {
		ResponseBuilder builder = Response.status(status).header("Access-Control-Allow-Origin", "*");
		if (obj != null) { builder = builder.entity(obj);}
		return builder.build();
	}
	
	public static boolean isUUIDValid(UUIDParam uuid, String paramName, ErrorResponse errors) {
		if (uuid == null) {
			errors.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			errors.addErrorMissingQueryParam(paramName);
			return false;
		} else {
			UUID facilityId = uuid.getUUID();
			if (facilityId == null) {
				errors.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				errors.addErrorBadUUID(uuid.getRawValue());
				return false;
			}
		}
		return true;
	}
	
	public static class UUIDParam {
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
