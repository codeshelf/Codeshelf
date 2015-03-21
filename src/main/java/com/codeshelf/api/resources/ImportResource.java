package com.codeshelf.api.resources;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Timestamp;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.LocationAliasCsvImporter;
import com.codeshelf.edi.OrderLocationCsvImporter;
import com.codeshelf.edi.OutboundOrderCsvImporter;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.validation.BatchResult;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/import")
public class ImportResource {

	@Context
	private ResourceContext resourceContext;	
	private ITenantPersistenceService persistence = TenantPersistenceService.getInstance();
	private AislesFileCsvImporter	aislesFileCsvImporter;
	//private OrderLocationCsvImporter orderLocationImporter;
	private LocationAliasCsvImporter locationAliasImporter;
	private OutboundOrderCsvImporter outboundOrderImporter;
	private InventoryCsvImporter inventoryImporter;
	
	@Inject
	public ImportResource(AislesFileCsvImporter aislesFileCsvImporter, 
		OrderLocationCsvImporter orderLocationImporter, 
		LocationAliasCsvImporter locationAliasImporter,  
		OutboundOrderCsvImporter outboundOrderImporter,
		InventoryCsvImporter inventoryImporter) {
		this.aislesFileCsvImporter = aislesFileCsvImporter;
		//this.orderLocationImporter = orderLocationImporter;
		this.locationAliasImporter = locationAliasImporter;
		this.outboundOrderImporter = outboundOrderImporter;
		this.inventoryImporter = inventoryImporter;
	}
	
	@POST
	@Path("/site/{facilityId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadSite(
		@PathParam("facilityId") String facilityId,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
		
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		try {
			persistence.beginTransaction(tenant);
			// make sure facility exists
			Facility facility = Facility.staticGetDao().findByPersistentId(tenant,facilityId);
			if (facility==null) {
				// facility not found
				return BaseResponse.buildResponse(null,404);
			}
			// process file
			Reader reader = new InputStreamReader(fileInputStream);
			boolean result = this.aislesFileCsvImporter.importAislesFileFromCsvStream(tenant,reader, facility, new Timestamp(System.currentTimeMillis()));
			if (result) {
				return BaseResponse.buildResponse(null,200);				
			}
			return BaseResponse.buildResponse(null,500);
		}
		catch (Exception e) {
			ErrorResponse errors = new ErrorResponse();
			errors.processException(e);
			return errors.buildResponse();
		} 
		finally {
			persistence.commitTransaction(tenant);
		}
	}
	
	@POST
	@Path("/locations/{facilityId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadLocations(
		@PathParam("facilityId") String facilityId,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {

		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		try {
			persistence.beginTransaction(tenant);
			// make sure facility exists
			Facility facility = Facility.staticGetDao().findByPersistentId(tenant,facilityId);
			if (facility==null) {
				// facility not found
				return BaseResponse.buildResponse(null,404);
			}
			Reader reader = new InputStreamReader(fileInputStream);
			boolean result = this.locationAliasImporter.importLocationAliasesFromCsvStream(tenant, reader, facility, new Timestamp(System.currentTimeMillis()));
			if (result) {
				return BaseResponse.buildResponse(null,200);				
			}
			return BaseResponse.buildResponse(null,500);
		}
		catch (Exception e) {
			ErrorResponse errors = new ErrorResponse();
			errors.processException(e);
			return errors.buildResponse();
		} 
		finally {
			persistence.commitTransaction(tenant);
		}
	}

	@SuppressWarnings("unused")
	@POST
	@Path("/orders/{facilityId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadOrders(
		@PathParam("facilityId") String facilityId,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {

		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		try {
			persistence.beginTransaction(tenant);
			// make sure facility exists
			Facility facility = Facility.staticGetDao().findByPersistentId(tenant,facilityId);
			if (facility==null) {
				// facility not found
				return BaseResponse.buildResponse(null,404);
			}
			Reader reader = new InputStreamReader(fileInputStream);
			BatchResult<Object> result = this.outboundOrderImporter.importOrdersFromCsvStream(tenant,reader, facility, new Timestamp(System.currentTimeMillis()));
			return BaseResponse.buildResponse(null,200);				
		}
		catch (Exception e) {
			ErrorResponse errors = new ErrorResponse();
			errors.processException(e);
			return errors.buildResponse();
		} 
		finally {
			persistence.commitTransaction(tenant);
		}
	}
	
	@POST
	@Path("/inventory/{facilityId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadInventory(
		@PathParam("facilityId") String facilityId,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {

		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();

		try {
			persistence.beginTransaction(tenant);
			// make sure facility exists
			Facility facility = Facility.staticGetDao().findByPersistentId(tenant,facilityId);
			if (facility==null) {
				// facility not found
				return BaseResponse.buildResponse(null,404);
			}
			Reader reader = new InputStreamReader(fileInputStream);
			boolean result = this.inventoryImporter.importSlottedInventoryFromCsvStream(tenant,reader, facility, new Timestamp(System.currentTimeMillis()));
			if (result) {
				return BaseResponse.buildResponse(null,200);				
			}
			return BaseResponse.buildResponse(null,500);
		}
		catch (Exception e) {
			ErrorResponse errors = new ErrorResponse();
			errors.processException(e);
			return errors.buildResponse();
		} 
		finally {
			persistence.commitTransaction(tenant);
		}
	}
}
