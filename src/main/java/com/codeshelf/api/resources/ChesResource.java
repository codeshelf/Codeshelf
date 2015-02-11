package com.codeshelf.api.resources;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.CheResource;
import com.sun.jersey.api.core.ResourceContext;

@Path("/ches")
public class ChesResource {
	@Context
	private ResourceContext resourceContext;

	@Path("{id}")
	public CheResource getManufacturer(@PathParam("id") UUIDParam uuidParam) throws Exception {
		CheResource r = resourceContext.getResource(CheResource.class);
	    r.setMUUIDParam(uuidParam);
	    return r;
	}

}
