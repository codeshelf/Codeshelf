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

import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.SftpConfiguration;
import com.codeshelf.model.domain.DropboxService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.model.domain.IronMqService;
import com.codeshelf.model.domain.SftpOrdersEdiService;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

public class EDIGatewaysResource {

	@Context
	private ResourceContext resourceContext;	
	
	@Setter
	private Facility facility;
	
	@Inject
	public EDIGatewaysResource()  {
	}

	@GET
	public Response getEdiServices() {
		facility.getEdiServices();
		return BaseResponse.buildResponse(new ArrayList<IEdiService>(facility.getEdiServices()));
	}
	@POST
	@Path("/{domainId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateEdiService(@PathParam("domainId") String domainId, MultivaluedMap<String, String> params) {
		IEdiService ediService = facility.findEdiService(domainId);
		try {
			IEdiService updatedEdiService = null;
			if (SftpOrdersEdiService.class.isAssignableFrom(ediService.getClass())) {
				updatedEdiService = updateSFTPOrders((SftpOrdersEdiService) ediService, params);
			} else if (IronMqService.class.isAssignableFrom(ediService.getClass())) {
				updatedEdiService = updateIronMqService((IronMqService) ediService, params);
			} else if (DropboxService.class.isAssignableFrom(ediService.getClass())) {
				updatedEdiService = updateDropboxService((DropboxService) ediService, params);
			}
			return BaseResponse.buildResponse(updatedEdiService);
		} catch(Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
	
	private IEdiService updateSFTPOrders(SftpOrdersEdiService ediService, MultivaluedMap<String, String> params) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		SftpConfiguration configuration = SftpConfiguration.updateFromMap(ediService.getConfiguration(), params);
		ediService.setConfiguration(configuration);
		SftpOrdersEdiService.staticGetDao().store(ediService);
		return ediService;
	}

	private IEdiService updateIronMqService(IronMqService ediService, MultivaluedMap<String, String> params) {
		ediService.storeCredentials(params.getFirst("projectId"), params.getFirst("token"));
		IronMqService.staticGetDao().store(ediService);
		return ediService;
	}

	private IEdiService updateDropboxService(DropboxService ediService, MultivaluedMap<String, String> params) {
		ediService.finishLink(params.getFirst("code"));
		DropboxService.staticGetDao().store(ediService);
		return ediService;
	}

	@GET
	@Path("/DROPBOX/link")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response dropbox() {
		DropboxService ediService = facility.findEdiService(DropboxService.class);
		try {
			String url = ediService.startLink();
			return BaseResponse.buildResponse(ImmutableMap.of("url", url));
		} catch(Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
}

