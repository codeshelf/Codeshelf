package com.codeshelf.api.resources.subresources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.model.domain.Worker;

public class WorkerResource {
	@Setter
	private Worker worker;
	
	@GET
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorker() {
		return BaseResponse.buildResponse(worker);
	}
	
	@PUT
	@RequiresPermissions("worker:edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUpdateWorker(Worker updatedWorker) {
		ErrorResponse errors = new ErrorResponse();
		try {
			//Set fields in the new objects for validation
			updatedWorker.setPersistentId(worker.getPersistentId());
			updatedWorker.setFacility(worker.getFacility());
			if (!updatedWorker.isValid(errors)){
				return errors.buildResponse();
			}
			//Update old object with new values
			worker.update(updatedWorker);
			//Save old object to the DB
			Worker.staticGetDao().store(worker);
			Worker savedWorker = Worker.staticGetDao().findByPersistentId(worker.getPersistentId());
			return BaseResponse.buildResponse(savedWorker);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		}
	}
}