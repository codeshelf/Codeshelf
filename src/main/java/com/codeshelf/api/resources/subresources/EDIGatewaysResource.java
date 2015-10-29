package com.codeshelf.api.resources.subresources;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.EdiExportService;
import com.codeshelf.edi.IEdiGateway;
import com.codeshelf.edi.SftpConfiguration;
import com.codeshelf.model.domain.DropboxGateway;
import com.codeshelf.model.domain.EdiGateway;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IronMqGateway;
import com.codeshelf.model.domain.SftpGateway;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

public class EDIGatewaysResource {


	private static final Logger	LOGGER = LoggerFactory.getLogger(EDIGatewaysResource.class);

	@Context
	private ResourceContext resourceContext;	
	
	@Setter
	private Facility facility;

	private EdiExportService	provider;
	
	@Inject
	public EDIGatewaysResource(EdiExportService provider)  {
		this.provider = provider;
	}

	@GET
	public Response getEdiGateways() {
		return BaseResponse.buildResponse(new ArrayList<IEdiGateway>(facility.getEdiGateways()));
	}
	
	@POST
	@Path("/{domainId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateEdiGateway(@PathParam("domainId") String domainId, MultivaluedMap<String, String> params) {
		IEdiGateway ediGateway = facility.findEdiGateway(domainId);
		try {
			IEdiGateway updatedEdiGateway = null;
			if (SftpGateway.class.isAssignableFrom(ediGateway.getClass())) {
				updatedEdiGateway = updateSftpGateway((SftpGateway) ediGateway, params);
			}else if (IronMqGateway.class.isAssignableFrom(ediGateway.getClass())) {
				updatedEdiGateway = updateIronMqGateway((IronMqGateway) ediGateway, params);
			} else if (DropboxGateway.class.isAssignableFrom(ediGateway.getClass())) {
				updatedEdiGateway = updateDropboxGateway((DropboxGateway) ediGateway, params);
			} else {
				LOGGER.warn("unexpected EDI class {}", ediGateway.getClass());
			}
			provider.updateEdiExporterSafe(facility);
			return BaseResponse.buildResponse(updatedEdiGateway);
		} catch(Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
	
	private IEdiGateway updateSftpGateway(SftpGateway ediGateway, MultivaluedMap<String, String> params) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SftpConfiguration configuration = SftpConfiguration.updateFromMap(ediGateway.getConfiguration(), params);
		ediGateway.setConfiguration(configuration);
		EdiGateway.staticGetDao().store(ediGateway);
		return ediGateway;
	}

	private IEdiGateway updateIronMqGateway(IronMqGateway ediGateway, MultivaluedMap<String, String> params) {
		ediGateway.storeCredentials(params.getFirst("projectId"), params.getFirst("token"), params.getFirst("active"));
		EdiGateway.staticGetDao().store(ediGateway);
		return ediGateway;
	}

	private IEdiGateway updateDropboxGateway(DropboxGateway ediGateway, MultivaluedMap<String, String> params) {
		ediGateway.finishLink(params.getFirst("code"));
		String activeStr = params.getFirst("active");
		ediGateway.setActive("true".equalsIgnoreCase(activeStr));
		EdiGateway.staticGetDao().store(ediGateway);
		return ediGateway;
	}

	@GET
	@Path("/DROPBOX/link")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response dropbox() {
		DropboxGateway ediGateway = facility.findEdiGateway(DropboxGateway.class);
		try {
			String url = ediGateway.startLink();
			return BaseResponse.buildResponse(ImmutableMap.of("url", url));
		} catch(Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
}

