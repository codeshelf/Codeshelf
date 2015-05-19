package com.codeshelf.api.resources.subresources;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Timestamp;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Setter;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.LocationAliasCsvImporter;
import com.codeshelf.edi.OrderLocationCsvImporter;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.validation.BatchResult;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

public class ImportResource {

	@Context
	private ResourceContext resourceContext;	
	private AislesFileCsvImporter	aislesFileCsvImporter;
	//private OrderLocationCsvImporter orderLocationImporter;
	private LocationAliasCsvImporter locationAliasImporter;
	private OutboundOrderPrefetchCsvImporter outboundOrderImporter;
	private InventoryCsvImporter inventoryImporter;
	
	@Setter
	private Facility facility;
	
	@Inject
	public ImportResource(AislesFileCsvImporter aislesFileCsvImporter, 
		OrderLocationCsvImporter orderLocationImporter, 
		LocationAliasCsvImporter locationAliasImporter,  
		OutboundOrderPrefetchCsvImporter outboundOrderImporter,
		InventoryCsvImporter inventoryImporter) {
		this.aislesFileCsvImporter = aislesFileCsvImporter;
		//this.orderLocationImporter = orderLocationImporter;
		this.locationAliasImporter = locationAliasImporter;
		this.outboundOrderImporter = outboundOrderImporter;
		this.inventoryImporter = inventoryImporter;
	}
	
	@POST
	@Path("/site")
	@RequiresPermissions("facility:edit")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadSite(
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
		try {
			// process file
			Reader reader = new InputStreamReader(fileInputStream);
			boolean result = this.aislesFileCsvImporter.importAislesFileFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
			if (result) {
				return BaseResponse.buildResponse(null,Status.OK);				
			}
			return BaseResponse.buildResponse(null, Status.INTERNAL_SERVER_ERROR);
		}
		catch (Exception e) {
			return new ErrorResponse().processException(e);
		} 
	}
	
	@POST
	@Path("/locations")
	@RequiresPermissions("facility:edit")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadLocations(
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
		try {
			Reader reader = new InputStreamReader(fileInputStream);
			boolean result = this.locationAliasImporter.importLocationAliasesFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
			if (result) {
				return BaseResponse.buildResponse(null,Status.OK);				
			}
			return BaseResponse.buildResponse(null, Status.INTERNAL_SERVER_ERROR);
		}
		catch (Exception e) {
			return new ErrorResponse().processException(e);
		} 
	}

	@POST
	@Path("/orders")
	@RequiresPermissions("order:import")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadOrders(
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
		try {
			Reader reader = new InputStreamReader(fileInputStream);
			BatchResult<Object> result = this.outboundOrderImporter.importOrdersFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
			return BaseResponse.buildResponse(result,Status.OK);				
		}
		catch (Exception e) {
			return new ErrorResponse().processException(e);
		} 
	}
	
	@POST
	@Path("/inventory")
	@RequiresPermissions("inventory:import")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadInventory(
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
		try {
			Reader reader = new InputStreamReader(fileInputStream);
			boolean result = this.inventoryImporter.importSlottedInventoryFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
			if (result) {
				return BaseResponse.buildResponse(null, Status.OK);				
			}
			return BaseResponse.buildResponse(null, Status.INTERNAL_SERVER_ERROR);
		}
		catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
}
