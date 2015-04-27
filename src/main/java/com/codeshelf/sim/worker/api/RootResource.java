package com.codeshelf.sim.worker.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

@Path("/")
public class RootResource {
	@SuppressWarnings("unused")
	private static final Logger	LOGGER				= LoggerFactory.getLogger(RootResource.class);
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response get() {
		return Response.ok("Codeshelf SIM API").build();
	}
	
	@GET
	@Path("/ches")
	public Response getSecurity() {
		// echo currently authenticated user for debugging
		UserContext user = CodeshelfSecurityManager.getCurrentUserContext();
		String response;
		if(user != null) {
			response = user.toString();
		} else {
			response = "unknown user";
		}
		return Response.ok(response).build();
	}


}
