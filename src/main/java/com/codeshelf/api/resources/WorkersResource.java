package com.codeshelf.api.resources;

import java.sql.Timestamp;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.resources.subresources.WorkerResource;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Worker;
import com.sun.jersey.api.core.ResourceContext;

@Path("/workers")
public class WorkersResource {
	@Context
	private ResourceContext resourceContext;
	
	private Facility facility;

	public void setFacility(Facility facility) {
		this.facility = facility;
	}

	
	@Path("{id}")
	@RequiresPermissions("worker:view")
	public WorkerResource getWorker(@PathParam("id") String idParam) throws Exception {
		Worker worker = null;
		if (facility != null) {
			String badgeIdParam = idParam;
			worker = Worker.staticGetDao().findByDomainId(facility, badgeIdParam);
			
		} else {
			UUID uuid = new UUIDParam(idParam).getValue();
			worker = Worker.staticGetDao().findByPersistentId(uuid);
		}
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
	public Response getAllWorkers(@QueryParam("badgeId") String domainId, @QueryParam("limit") Integer limit) {
		Criteria criteria= Worker.staticGetDao().createCriteria();

		if (facility != null) {
			criteria.add(Property.forName("parent").eq(facility));
		}
		if (domainId !=  null) {
			criteria.add(GenericDaoABC.createSubstringRestriction("domainId", domainId));
		}
		ResultDisplay<Worker> results = Worker.staticGetDao().findByCriteriaQueryPartial(criteria, Order.asc("domainId"), limit);
		return BaseResponse.buildResponse(results);
	}
	
	@POST
	@RequiresPermissions("worker:edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createWorker(Worker worker) {
		ErrorResponse errors = new ErrorResponse();
		try {
			worker.setParent(facility);
			if (worker.getActive() == null) {
				worker.setActive(true);
			}
			worker.setUpdated(new Timestamp(System.currentTimeMillis()));
			if (!worker.isValid(errors)) {
				return errors.buildResponse();
			}
			Worker existingWorker = Worker.findTenantWorker(worker.getDomainId());
			if (existingWorker != null){
				existingWorker.update(worker);
				worker = existingWorker;
			}
			UUID id = worker.getPersistentId();
			Worker.staticGetDao().store(worker);
			Worker createdWorker = Worker.staticGetDao().findByPersistentId(id);
			return BaseResponse.buildResponse(createdWorker);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}


}
