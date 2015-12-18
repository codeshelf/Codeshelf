package com.codeshelf.api.resources;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
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

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.CheResource;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.api.responses.ResultDisplay;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.ws.server.CsServerEndPoint;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

@Path("/ches")
public class ChesResource {
	@Context
	private ResourceContext resourceContext;
	private NotificationBehavior notificationBehavior;
	private static ScriptEngine engine;

	static {
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("groovy");

	}

	@Inject
	ChesResource(NotificationBehavior notificationBehavior) {
		this.notificationBehavior = notificationBehavior;
	}
	
	@GET
	@Path("/pools")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, ExecutorService> getExecutors() {
		return CsServerEndPoint.getDevicePools();
	}

	@GET
	@Path("/poolsstring")
	public String getExecutorsAsString() {
		return CsServerEndPoint.getDevicePools().toString();
	}

	@POST
	@Path("/pools")
	@Produces(MediaType.APPLICATION_JSON)
	public Object scriptPool(String script) throws ScriptException {
		SimpleBindings bindings = new SimpleBindings();
		bindings.put("pools", CsServerEndPoint.getDevicePools());
		return engine.eval(script, bindings);
	}

	
	@Path("{id}")
	@RequiresPermissions("companion:view")
	public CheResource findChe(@PathParam("id") UUIDParam uuidParam) throws Exception {
		Che che = Che.staticGetDao().findByPersistentId(uuidParam.getValue());
		if (che == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		CheResource r = resourceContext.getResource(CheResource.class);
	    r.setChe(che);
	    return r;
	}

	@GET
	@Path("/{id}/events")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEvents(@PathParam("id") UUIDParam uuidParam, @QueryParam("limit") Integer limit) {
		ResultDisplay<EventDisplay> results = this.notificationBehavior.getEventsForCheId(uuidParam, limit);
		return BaseResponse.buildResponse(results);
	}

	
}
