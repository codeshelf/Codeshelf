package com.codeshelf.api.resources.subresources;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Setter;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.TimestampParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.LocationAliasCsvImporter;
import com.codeshelf.edi.OrderLocationCsvImporter;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.edi.WorkerCsvImporter;
import com.codeshelf.model.domain.DataImportReceipt;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.validation.BatchResult;
import com.google.common.collect.Lists;
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
	private WorkerCsvImporter	workerImporter;
	
	@Setter
	private Facility facility;
	
	@Inject
	public ImportResource(AislesFileCsvImporter aislesFileCsvImporter, 
		OrderLocationCsvImporter orderLocationImporter, 
		LocationAliasCsvImporter locationAliasImporter,  
		OutboundOrderPrefetchCsvImporter outboundOrderImporter,
		InventoryCsvImporter inventoryImporter,
		WorkerCsvImporter workerImporter) {
		this.aislesFileCsvImporter = aislesFileCsvImporter;
		//this.orderLocationImporter = orderLocationImporter;
		this.locationAliasImporter = locationAliasImporter;
		this.outboundOrderImporter = outboundOrderImporter;
		this.inventoryImporter = inventoryImporter;
		this.workerImporter = workerImporter;
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
	@Path("/workers")
	@RequiresPermissions("order:import")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadWorkers(
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
		
		try {
			long receivedTime = System.currentTimeMillis();
			Reader reader = new InputStreamReader(fileInputStream);
			
			boolean result = this.workerImporter.importWorkersFromCsvStream(reader, facility, new java.sql.Timestamp(receivedTime));
			if (result) {
				return BaseResponse.buildResponse(null, Status.OK);				
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
			long receivedTime = System.currentTimeMillis();
			Reader reader = new InputStreamReader(fileInputStream);
			
			BatchResult<Object> results = this.outboundOrderImporter.importOrdersFromCsvStream(reader, facility, new Timestamp(System.currentTimeMillis()));
			String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
			this.outboundOrderImporter.persistDataReceipt(facility, username, contentDispositionHeader.getFileName(), receivedTime, results);
			return BaseResponse.buildResponse(results, Status.OK);				
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
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getImportReceipts(@QueryParam("startTimestamp") TimestampParam startTimestamp, @QueryParam("endTimestamp") TimestampParam endTimestamp) {
		try {
			ArrayList<Criterion> filter = Lists.newArrayList();
			filter.add(Restrictions.eq("parent", facility));
			if (startTimestamp != null) {
				filter.add(Restrictions.ge("started", startTimestamp.getValue()));
			}
			if (endTimestamp != null) {
				filter.add(Restrictions.le("started", endTimestamp.getValue()));
			}
			List<DataImportReceipt> receipts = DataImportReceipt.staticGetDao().findByFilter(filter);
			return BaseResponse.buildResponse(receipts);
		}
		catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
		
	}
	
}
