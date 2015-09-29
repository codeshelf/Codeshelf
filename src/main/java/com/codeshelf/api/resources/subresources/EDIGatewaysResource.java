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
	public Response getEdiServices() {
		facility.getEdiServices();
		return BaseResponse.buildResponse(new ArrayList<IEdiGateway>(facility.getEdiServices()));
	}
	@POST
	@Path("/{domainId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateEdiService(@PathParam("domainId") String domainId, MultivaluedMap<String, String> params) {
		IEdiGateway ediService = facility.findEdiService(domainId);
		try {
			IEdiGateway updatedEdiService = null;
			if (SftpGateway.class.isAssignableFrom(ediService.getClass())) {
				updatedEdiService = updateSftpGateway((SftpGateway) ediService, params);
			}else if (IronMqGateway.class.isAssignableFrom(ediService.getClass())) {
				updatedEdiService = updateIronMqGateway((IronMqGateway) ediService, params);
			} else if (DropboxGateway.class.isAssignableFrom(ediService.getClass())) {
				updatedEdiService = updateDropboxGateway((DropboxGateway) ediService, params);
			} else {
				LOGGER.warn("unexpected EDI class {}", ediService.getClass());
			}
			provider.ediExportServiceUpdated(facility);
			return BaseResponse.buildResponse(updatedEdiService);
		} catch(Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
	
	private IEdiGateway updateSftpGateway(SftpGateway ediService, MultivaluedMap<String, String> params) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SftpConfiguration configuration = SftpConfiguration.updateFromMap(ediService.getConfiguration(), params);
		ediService.setConfiguration(configuration);
		ediService.getDao().store(ediService);
		return ediService;
	}

	private IEdiGateway updateIronMqGateway(IronMqGateway ediService, MultivaluedMap<String, String> params) {
		ediService.storeCredentials(params.getFirst("projectId"), params.getFirst("token"), params.getFirst("active"));
		IronMqGateway.staticGetDao().store(ediService);
		return ediService;
	}

	private IEdiGateway updateDropboxGateway(DropboxGateway ediService, MultivaluedMap<String, String> params) {
		ediService.finishLink(params.getFirst("code"));
		DropboxGateway.staticGetDao().store(ediService);
		return ediService;
	}

	@GET
	@Path("/DROPBOX/link")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response dropbox() {
		DropboxGateway ediService = facility.findEdiService(DropboxGateway.class);
		try {
			String url = ediService.startLink();
			return BaseResponse.buildResponse(ImmutableMap.of("url", url));
		} catch(Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
}

