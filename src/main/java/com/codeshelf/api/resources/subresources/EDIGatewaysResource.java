package com.codeshelf.api.resources.subresources;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.CRUDResource;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.EdiExportService;
import com.codeshelf.edi.SftpConfiguration;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.DropboxGateway;
import com.codeshelf.model.domain.EdiGateway;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IronMqGateway;
import com.codeshelf.model.domain.SftpGateway;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public class EDIGatewaysResource extends CRUDResource<EdiGateway, Facility> {


	private static final Logger	LOGGER = LoggerFactory.getLogger(EDIGatewaysResource.class);

	private EdiExportService	provider;
	
	@Inject
	public EDIGatewaysResource(EdiExportService provider)  {
		super(EdiGateway.class, "edi",  Optional.<Set<String>>absent());
		this.provider = provider;
	}
	
	@Override
	protected ResultDisplay<EdiGateway> doFindByParent(Facility parent, MultivaluedMap<String, String> params) {
		return new ResultDisplay<>(parent.getEdiGateways());
	}
	
	@Override
	protected EdiGateway doUpdate(EdiGateway ediGateway, MultivaluedMap<String, String> params) throws ReflectiveOperationException {
		EdiGateway updatedEdiGateway = null;
		if (SftpGateway.class.isAssignableFrom(ediGateway.getClass())) {
			updatedEdiGateway = updateSftpGateway((SftpGateway) ediGateway, params);
		}else if (IronMqGateway.class.isAssignableFrom(ediGateway.getClass())) {
			updatedEdiGateway = updateIronMqGateway((IronMqGateway) ediGateway, params);
		} else if (DropboxGateway.class.isAssignableFrom(ediGateway.getClass())) {
			updatedEdiGateway = updateDropboxGateway((DropboxGateway) ediGateway, params);
		} else {
			LOGGER.warn("unexpected EDI class {}", ediGateway.getClass());
		}
		provider.updateEdiExporterSafe(getParent());
		return updatedEdiGateway;
	}
	
	private EdiGateway updateSftpGateway(SftpGateway ediGateway, MultivaluedMap<String, String> params) throws ReflectiveOperationException {
		SftpConfiguration configuration = SftpConfiguration.updateFromMap(ediGateway.getConfiguration(), params);
		ediGateway.setConfiguration(configuration);
		return ediGateway;
	}

	private EdiGateway updateIronMqGateway(IronMqGateway ediGateway, MultivaluedMap<String, String> params) {
		ediGateway.storeCredentials(params.getFirst("projectId"), params.getFirst("token"), params.getFirst("active"));
		return ediGateway;
	}

	private EdiGateway updateDropboxGateway(DropboxGateway ediGateway, MultivaluedMap<String, String> params) {
		ediGateway.finishLink(params.getFirst("code"));
		String activeStr = params.getFirst("active");
		ediGateway.setActive("true".equalsIgnoreCase(activeStr));
		return ediGateway;
	}

	@GET
	@Path("/DROPBOX/link")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response dropbox() {
		DropboxGateway ediGateway = getParent().findEdiGateway(DropboxGateway.class);
		try {
			String url = ediGateway.startLink();
			return BaseResponse.buildResponse(ImmutableMap.of("url", url));
		} catch(Exception e) {
			return new ErrorResponse().processException(e);
		}
	}

	@Override
	protected String getNewDomainIdExistsMessage(String inDomainId) {
		return "Another EDI gateway of type " + inDomainId + " already exists";
	}
}

