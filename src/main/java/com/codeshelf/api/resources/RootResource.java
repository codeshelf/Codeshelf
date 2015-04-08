package com.codeshelf.api.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;

@Path("/")
@RequiresAuthentication
public class RootResource {

	@GET
	public String get(){
		return "Codeshelf HTTP API"; //todo return hyperlinked response to other top level calls
		
	}
}
