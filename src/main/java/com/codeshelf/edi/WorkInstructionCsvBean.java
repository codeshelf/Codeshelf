/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * This is our "native" WI bean. We have default header to match the default csv content
 *
 */
public class WorkInstructionCsvBean extends ExportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(WorkInstructionCsvBean.class);

	public WorkInstructionCsvBean() {
	};
	
	// Potentially missing fields: description, gtin.  lotId is probably superfluous.
	// Note that for bean export, null field will export as "null" instead of "". We want "". See handling on pickerId

	@Getter @Setter @Expose
	protected UUID		facilityPersistentId;
	@Getter @Setter @Expose
	protected String	facilityId;
	@Getter @Setter @Expose
	protected String	workInstructionId;
	@Getter @Setter @Expose
	protected String	type;
	@Getter @Setter @Expose
	protected String	status;
	@Getter @Setter @Expose
	protected String	orderGroupId;
	@Getter @Setter @Expose
	protected String	orderId;
	@Getter @Setter @Expose
	protected String	containerId;
	@Getter @Setter @Expose
	protected String	itemId;
	@Getter @Setter @Expose
	protected String	uom;
	@Getter @Setter @Expose
	protected String	lotId;
	@Getter @Setter @Expose
	protected String	cheId;
	@Getter @Setter @Expose
	protected String	locationId;
	@Getter @Setter @Expose
	protected String	pickerId;
	@Getter @Setter @Expose
	protected String	planQuantity;
	@Getter @Setter @Expose
	protected String	actualQuantity;
	@Getter @Setter @Expose
	protected String	assigned;
	@Getter @Setter @Expose
	protected String	started;
	@Getter @Setter @Expose
	protected String	completed;
	
	@Getter @Setter
	private UUID		persistentId;

	/**
	 * This does NOT automatically define the order the fields are written out. Matches the old IronMQ format
	 */
	public static String getCsvHeaderMatchingBean(){
		return "facilityId, workInstructionId, type, status, orderGroupId, orderId, containerId,"
				+ "itemId, uom, lotId, locationId, pickerId, planQuantity, actualQuantity, cheId,"
				+ "assigned, started, completed"; // no version here
	}
	
	/**
	 * This defines the order the fields are written out. Matches the old IronMQ format. This should match getCsvHeaderMatchingBean 
	 */
	public String getDefaultCsvContent(){
		return facilityId +","+ workInstructionId+","+ type+","+ status+","+ orderGroupId+","+ orderId+","+ containerId
				+","+ itemId+","+ uom+","+ lotId+","+ locationId+","+ pickerId+","+ planQuantity+","+ actualQuantity+","+ cheId
				+","+ assigned+","+ started+","+ completed; 
	}
	
	public WorkInstructionCsvBean(WorkInstruction inWi) {
		setFacilityPersistentId(inWi.getParentPersistentId());
		setFacilityId(inWi.getParent().getDomainId());
		setWorkInstructionId(inWi.getDomainId());
		setType(inWi.getType().toString());
		setStatus(inWi.getStatus().toString());

		// groups are optional!
		String groupStr = "";
		// from v5, housekeeping wi may have no detail
		if (inWi.getOrderDetail() != null) {
			OrderGroup theGroup = inWi.getOrderDetail().getParent().getOrderGroup();
			if (theGroup != null)
				groupStr = theGroup.getDomainId();
		}
		setOrderGroupId(groupStr);

		// from v5, housekeeping wi may have no detail
		String orderStr = "";
		if (inWi.getOrderDetail() != null)
			orderStr = inWi.getOrderDetail().getOrderId();
		setOrderId(orderStr);

		setContainerId(inWi.getContainerId());
		setItemId(inWi.getItemId());
		setUom(inWi.getUomMasterId());
		setLotId("");

		// Use the denormalized version on the work instruction. That usually will match the location alias that customer is using.
		// Or, for location based pick, will match the location in the order detail
		String locationStr = inWi.getPickInstruction();
		setLocationId(locationStr);

		String picker = getPickerId(); // this field is nullable on work instruction
		if (picker == null)
			picker = "";
		setPickerId(picker);

		if (inWi.getPlanQuantity() != null) {
			setPlanQuantity(String.valueOf(inWi.getPlanQuantity()));
		}
		if (inWi.getActualQuantity() != null) {
			setActualQuantity(String.valueOf(inWi.getActualQuantity()));
		}

		setCheId(inWi.getAssignedCheName());
		setAssigned(formatDate(inWi.getAssigned()));
		setStarted(formatDate(inWi.getStarted()));
		setCompleted(formatDate(inWi.getCompleted()));
	}
	
	@Override
	public String toString() {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJson(this);
	}

	static public WorkInstructionCsvBean fromString(String string) {
		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		WorkInstructionCsvBean bean = mGson.fromJson(string, WorkInstructionCsvBean.class);
		return bean;
	}
}
