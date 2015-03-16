package com.codeshelf.manager.api;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.User;
import com.codeshelf.security.AuthFilter;

@Path("/")
public class RootResource {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(RootResource.class);
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response get() {
		return Response.ok("Codeshelf TMS").build();
	}
	
	@GET
	@Path("/security")
	@Produces(MediaType.TEXT_PLAIN)
	public Response get(@Context HttpServletRequest request) {
		// echo currently authenticated user for debugging

		User user = (User) request.getAttribute(AuthFilter.REQUEST_ATTR);
		String response;
		if(user != null) {
			response = user.toString();
		} else {
			response = "unknown user";
		}
		response += " - ";
		Enumeration<String> headers = request.getHeaderNames();
		while(headers.hasMoreElements()) {
			String header = headers.nextElement();
			response += header + " ";
		}
				
		return Response.ok(response).build();
	}	
	
	public static Map<String, String> validFieldsOnly(MultivaluedMap<String, String> userParams, Set<String> validFields) {
		Map<String, String> result = new HashMap<String, String>();
		boolean error = false;

		for (String key : userParams.keySet()) {
			if (validFields.contains(key)) {
				List<String> values = userParams.get(key);
				if (values == null) {
					LOGGER.error("null value for key {}", key); // this shouldn't happen
					error = true;
					break;
				} else if (values.isEmpty()) {
					LOGGER.error("no values for key {}", key); // this shouldn't happen
					error = true;
					break;
				} else if (values.size() != 1) {
					LOGGER.warn("multiple values for key {}", key); // bad form input 
					error = true;
					break;
				} else {
					result.put(key, values.get(0)); // ok field
				}
			} else {
				LOGGER.warn("unrecognized field {}", key);
				error = true;
				break;
			}
		}
		if(result.isEmpty()) {
			LOGGER.warn("no form data");
			error = true;
		}

		return error ? null : result;
	}


}
