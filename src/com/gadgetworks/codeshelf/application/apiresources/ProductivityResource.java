package com.gadgetworks.codeshelf.application.apiresources;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.gadgetworks.codeshelf.model.domain.ProductivitySummary;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ServiceMethodResponse;

@Path("/productivity")
public class ProductivityResource {
	private PersistenceService persistence = PersistenceService.getInstance();
	
	@GET
	@Path("/summary")
	@Produces(MediaType.APPLICATION_JSON)
	public ProductivitySummary getEmployee(@QueryParam("facilityId") UUID facilityId) {
		persistence.beginTenantTransaction();
		ProductivitySummary productivitySummary = WorkService.getProductivitySummary(facilityId);
		persistence.commitTenantTransaction();
		return productivitySummary;
	}
}
