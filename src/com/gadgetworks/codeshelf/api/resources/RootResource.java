package com.gadgetworks.codeshelf.api.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class RootResource {

	@GET
	public String get(){
		return "Codeshelf HTTP API"; //todo return hyperlinked response to other top level calls
		
	}
}
