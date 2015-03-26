package com.codeshelf.api.resources;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.CheResource;
import com.codeshelf.model.domain.Che;
import com.sun.jersey.api.core.ResourceContext;

@Path("/ches")
public class ChesResource {
	@Context
	private ResourceContext resourceContext;

	@Path("{id}")
	public CheResource findChe(@PathParam("id") UUIDParam uuidParam) throws Exception {
		Che che = Che.staticGetDao().findByPersistentId(uuidParam.getValue());
		if (che == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		CheResource r = resourceContext.getResource(CheResource.class);
	    r.setChe(che);
	    return r;
	}

}
