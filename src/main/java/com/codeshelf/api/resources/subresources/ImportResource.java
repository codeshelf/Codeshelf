package com.codeshelf.api.resources.subresources;

import static com.codeshelf.model.dao.GenericDaoABC.createIntervalRestriction;
import static com.codeshelf.model.dao.GenericDaoABC.createSubstringRestriction;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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
import org.joda.time.Interval;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.IntervalParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.LocationAliasCsvImporter;
import com.codeshelf.edi.OrderLocationCsvImporter;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.edi.WorkerCsvImporter;
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ImportReceipt;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.validation.BatchResult;
import com.google.common.base.Strings;
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
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
        @FormDataParam("deleteOldOrders") @DefaultValue(value = "false") boolean deleteOldOrders) {
		try {
			long receivedTime = System.currentTimeMillis();
			Reader reader = new InputStreamReader(fileInputStream);
			
			BatchResult<Object> results = this.outboundOrderImporter.importOrdersFromCsvStream(reader, facility, new Timestamp(receivedTime), deleteOldOrders);
			String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
			this.outboundOrderImporter.persistDataReceipt(facility, username, contentDispositionHeader.getFileName(), receivedTime, EdiTransportType.APP, results);
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
	public Response getImportReceipts(@QueryParam("received") IntervalParam received, 
									  @QueryParam("orderIds") String orderIds, 
									  @QueryParam("itemIds") String itemIds,
									  @QueryParam("gtins") String gtins) {
		try {
			ArrayList<Criterion> filter = Lists.newArrayList();
			filter.add(Restrictions.eq("parent", facility));
			if (received !=null) {
				Interval receivedInterval = received.getValue();
				if (receivedInterval != null) {
					filter.add(createIntervalRestriction("received", receivedInterval));
				}
			}
			if (!Strings.isNullOrEmpty(orderIds)) {
				filter.add(createSubstringRestriction("orderIds", orderIds));
			}
			if (!Strings.isNullOrEmpty(itemIds)) {
				filter.add(createSubstringRestriction("itemIds", itemIds));
			}
			if (!Strings.isNullOrEmpty(gtins)) {
				filter.add(createSubstringRestriction("gtins", gtins));
			}
			List<ImportReceipt> receipts = ImportReceipt.staticGetDao().findByFilter(filter);
			return BaseResponse.buildResponse(receipts);
		}
		catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
		
	}
	
}
