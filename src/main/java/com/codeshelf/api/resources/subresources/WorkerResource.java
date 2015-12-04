package com.codeshelf.api.resources.subresources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Setter;

import org.apache.shiro.authz.annotation.RequiresPermissions;

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
	public Response updateWorker(Worker updatedWorker) {
		ErrorResponse errors = new ErrorResponse();
		try {
			if (!updatedWorker.isValid(errors)){ 
				return errors.buildResponse();
			}
			Worker workerWithSameBadge = Worker.findTenantWorker(updatedWorker.getBadgeId());
			if (workerWithSameBadge != null && !worker.equals(workerWithSameBadge)) {
				errors.addError("Another worker with badge " + updatedWorker.getBadgeId() + " already exists");
				return errors.buildResponse();
			}
			//Update old object with new values
			worker.update(updatedWorker);
			//Save old object to the DB
			Worker.staticGetDao().store(worker);
			Worker savedWorker = Worker.staticGetDao().findByPersistentId(worker.getPersistentId());
			return BaseResponse.buildResponse(savedWorker);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}
	
	@DELETE
	@RequiresPermissions("worker:edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteWorker(Worker updatedWorker) {
		ErrorResponse errors = new ErrorResponse();
		try {
			Worker.staticGetDao().delete(worker);
			return BaseResponse.buildResponse("Not yet implemented");
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

}
