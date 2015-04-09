package com.codeshelf.api.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

@Path("/")
@RequiresAuthentication
public class RootResource {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String get(){
		return "Codeshelf HTTP API"; //todo return hyperlinked response to other top level calls
		
	}
}
