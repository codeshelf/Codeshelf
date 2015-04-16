package com.codeshelf.api.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.WorkerResource;
import com.codeshelf.model.domain.Worker;
import com.sun.jersey.api.core.ResourceContext;

@Path("/workers")
public class WorkersResource {
	@Context
	private ResourceContext resourceContext;
	
	@Path("{id}")
	@RequiresPermissions("worker:view")
	public WorkerResource getWorker(@PathParam("id") UUIDParam uuidParam) throws Exception {
		Worker worker = Worker.staticGetDao().findByPersistentId(uuidParam.getValue());
		if (worker == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		WorkerResource r = resourceContext.getResource(WorkerResource.class);
	    r.setWorker(worker);
	    return r;
	}
	
	@GET
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllWorkers() {
		List<Worker> workers = Worker.staticGetDao().getAll();
		return BaseResponse.buildResponse(workers);
	}
}
