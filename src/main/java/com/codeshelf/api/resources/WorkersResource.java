package com.codeshelf.api.resources;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.CRUDResource;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.ParameterUtils;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.behavior.NotificationBehavior.HistogramParams;
import com.codeshelf.behavior.NotificationBehavior.HistogramResult;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

public class WorkersResource extends CRUDResource<Worker, Facility>{
	private static Set<String> updatableProperties = ImmutableSet.of(
			"active",
			"firstName",
			"lastName",
			"middleInitial",
			"groupName",
			"hrId");
	
	@Context
	private ResourceContext resourceContext;

	private WorkBehavior workBehavior;
	private NotificationBehavior notificationBehavior;
	
	@Inject
	public WorkersResource(WorkBehavior workBehavior, NotificationBehavior notificationBehavior) {
		super(Worker.class, "worker", Optional.of(updatableProperties));
		this.workBehavior = workBehavior;
		this.notificationBehavior = notificationBehavior;
	}

	@Path("/{id}/events")
	public EventsResource getEvents(@PathParam("id") String id) {
		EventsResource r = resourceContext.getResource(EventsResource.class);
		r.setWorker(doGetById(id));
		return r;
	}
	
	@GET
	@Path("/{id}/workinstructions")
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkInstructions(@PathParam("id") String id) {
		ErrorResponse errors = new ErrorResponse();

		try {
			List<WorkInstruction> workInstructions = workBehavior.getWorkInstructions(doGetById(id));
			return BaseResponse.buildResponse(new ResultDisplay<WorkInstruction>(workInstructions));
		} catch (Exception e) {
			return errors.processException(e);
		} 
	}
	
	@GET
	@Path("/{id}/events/histogram")
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEventHistogram(@PathParam("id") String id, @Context UriInfo uriInfo) throws Exception {
		ErrorResponse errors = new ErrorResponse();
		try {
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
			HistogramParams params = new HistogramParams(queryParams);  
			HistogramResult result = notificationBehavior.pickRateHistogram(params, doGetById(id));
			return BaseResponse.buildResponse(result);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	
	@Override
	protected ResultDisplay<Worker> doFindByParent(Facility facility, MultivaluedMap<String, String> params) {
		String domainId = ParameterUtils.getDomainId(params);
		Integer limit = ParameterUtils.getInteger(params, "limit");
		Criteria criteria= Worker.staticGetDao().createCriteria();

		if (facility != null) {
			criteria.add(Property.forName("parent").eq(facility));
		}
		if (domainId !=  null) {
			criteria.add(GenericDaoABC.createSubstringRestriction("domainId", domainId));
		}
		ResultDisplay<Worker> results = Worker.staticGetDao().findByCriteriaQueryPartial(criteria, Order.asc("domainId"), limit);
		return results;
	}

	@Override
	protected Worker doNewObject(MultivaluedMap<String, String> params) throws ReflectiveOperationException {
		Worker newWorker = super.doNewObject(params);
		newWorker.setUpdated(new Timestamp(System.currentTimeMillis()));
		return newWorker;
	}

	@Override
	protected Worker doUpdate(Worker bean, MultivaluedMap<String, String> params) throws ReflectiveOperationException {
		Worker updatedWorker = super.doUpdate(bean, params);
		updatedWorker.setUpdated(new Timestamp(System.currentTimeMillis()));
		return updatedWorker;
	}

	@Override
	protected String getNewDomainIdExistsMessage(String inDomainId) {
		return "Another worker with badge " + inDomainId + " already exists";
	}
	


	
}
