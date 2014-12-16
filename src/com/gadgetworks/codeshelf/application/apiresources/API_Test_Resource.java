package com.gadgetworks.codeshelf.application.apiresources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/apitest")
public class API_Test_Resource {
	@GET
	@Path("/gettest")
	@Produces(MediaType.APPLICATION_JSON)
	public API_Test_Object getEmployee() {
		return new API_Test_Object();
	}
}
