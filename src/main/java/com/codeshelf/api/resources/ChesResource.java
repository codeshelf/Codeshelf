package com.codeshelf.api.resources;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.codeshelf.api.resources.subresources.CheResource;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.ws.server.CsServerEndPoint;
import com.sun.jersey.api.core.ResourceContext;

import lombok.Setter;

@Path("/ches")
public class ChesResource {
	@Context
	private ResourceContext resourceContext;

	@Setter
	private Facility facility;
	private static ScriptEngine engine;

	static {
		ScriptEngineManager factory = new ScriptEngineManager();
		engine = factory.getEngineByName("groovy");

	}

	@GET
	@RequiresPermissions("che:view")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Che> getChes() {
		if(facility != null) {
			Criteria cheCriteria = Che.staticGetDao().createCriteria();
			cheCriteria.createCriteria("parent", "network").add(Restrictions.eq("parent", facility));
			List<Che> ches = Che.staticGetDao().findByCriteriaQuery(cheCriteria);
			return ches;
		} else {
			List<Che> ches = Che.staticGetDao().getAll();
			return ches;
		}
	}

	@Path("/{id}")
	public CheResource findChe(@PathParam("id") String idParam) throws Exception {
		Che che = null;
		try {
			UUID uuid = UUID.fromString(idParam);
			che = Che.staticGetDao().findByPersistentId(uuid);
		} catch(Exception e) {
			Criteria cheCriteria = Che.staticGetDao().createCriteria()
					.add(Restrictions.eq("domainId", idParam));
			cheCriteria.createCriteria("parent", "network")
				.add(Restrictions.eq("parent", facility));
			List<Che> ches = Che.staticGetDao().findByCriteriaQuery(cheCriteria);

			che = ches.get(0);
		}
		if (che == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		CheResource r = resourceContext.getResource(CheResource.class);
	    r.setChe(che);
	    return r;
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
	
}
