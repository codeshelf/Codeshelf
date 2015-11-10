/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.DomainObjectTreeABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * This is our "native" WI bean. We have default header to match the default csv content
 *
 */
@Entity
@Table(name = "work_instruction_bean")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class WorkInstructionCsvBean extends DomainObjectTreeABC<Facility>{

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(WorkInstructionCsvBean.class);
	private static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	// Potentially missing fields: description, gtin.  lotId is probably superfluous.
	// Note that for bean export, null field will export as "null" instead of "". We want "". See handling on pickerId

	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter @Setter
	protected Facility		parent;

	@Column(nullable = false)
	@Getter @Setter
	private Boolean			active;
	
	@Column(nullable = false)
	@Getter @Setter
	private Timestamp 		updated;
	
	@Setter @Getter @Expose @Column(name = "line_number")
	private Integer 	lineNumber;
	@Getter @Setter @Expose @Column(name = "facility_id")
	protected String	facilityId;
	@Getter @Setter @Expose @Column(name = "work_instruction_id")
	protected String	workInstructionId;
	@Getter @Setter @Expose
	protected String	type;
	@Getter @Setter @Expose
	protected String	status;
	@Getter @Setter @Expose @Column(name = "order_group_id")
	protected String	orderGroupId;
	@Getter @Setter @Expose @Column(name = "order_id")
	protected String	orderId;
	@Getter @Setter @Expose @Column(name = "container_id")
	protected String	containerId;
	@Getter @Setter @Expose @Column(name = "item_id")
	protected String	itemId;
	@Getter @Setter @Expose
	protected String	uom;
	@Getter @Setter @Expose @Column(name = "lot_id")
	protected String	lotId;
	@Getter @Setter @Expose @Column(name = "che_id")
	protected String	cheId;
	@Getter @Setter @Expose @Column(name = "location_id")
	protected String	locationId;
	@Getter @Setter @Expose @Column(name = "badge")
	protected String	badge;
	@Getter @Setter @Expose @Column(name = "worker")
	protected String	worker;
	@Getter @Setter @Expose @Column(name = "plan_quantity")
	protected String	planQuantity;
	@Getter @Setter @Expose @Column(name = "actual_quantity")
	protected String	actualQuantity;
	@Getter @Setter @Expose @Column(name = "detail_quantity")
	protected String	detailQuantity;
	@Getter @Setter @Expose
	protected String	assigned;
	@Getter @Setter @Expose
	protected String	started;
	@Getter @Setter @Expose
	protected String	completed;
	
	public static class WorkInstructionCsvBeanDao extends GenericDaoABC<WorkInstructionCsvBean> implements ITypedDao<WorkInstructionCsvBean> {
		public final Class<WorkInstructionCsvBean> getDaoClass() {
			return WorkInstructionCsvBean.class;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public final ITypedDao<WorkInstructionCsvBean> getDao() {
		return staticGetDao();
	}


	public static ITypedDao<WorkInstructionCsvBean> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(WorkInstructionCsvBean.class);
	}
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "WIBean";
	}


	@Override
	public Facility getFacility() {
		return getParent();
	}

	/**
	 * This does NOT automatically define the order the fields are written out. Matches the old IronMQ format
	 */
	public static String getCsvHeaderMatchingBean(){
		return "facilityId, workInstructionId, type, status, orderGroupId, orderId, containerId,"
				+ "itemId, uom, lotId, locationId, badge, worker, planQuantity, actualQuantity, detailQuantity,"
				+ "cheId, assigned, started, completed"; // no version here
	}
	
	/**
	 * This defines the order the fields are written out. Matches the old IronMQ format. This should match getCsvHeaderMatchingBean 
	 */
	public String getDefaultCsvContent(){
		return facilityId +","+ workInstructionId+","+ type+","+ status+","+ orderGroupId+","+ orderId+","+ containerId
				+","+ itemId+","+ uom+","+ lotId+","+ locationId+","+ badge+","+ worker+ ","+planQuantity+","+ actualQuantity+","+ detailQuantity
				+"," + cheId + ","+ assigned+","+ started+","+ completed; 
	}
	
	public WorkInstructionCsvBean() {
	};
	
	public WorkInstructionCsvBean(WorkInstruction inWi) {
		setDomainId(getDefaultDomainIdPrefix() + "_" + System.currentTimeMillis());
		setParent(inWi.getFacility());
		setActive(true);
		setUpdated(new Timestamp(System.currentTimeMillis()));
		setFacilityId(inWi.getParent().getDomainId());
		setWorkInstructionId(inWi.getDomainId());
		setType(inWi.getType().toString());
		setStatus(inWi.getStatus().toString());

		OrderDetail detail = inWi.getOrderDetail();
		// groups are optional!
		String groupStr = "";
		// from v5, housekeeping wi may have no detail
		if (detail != null) {
			OrderGroup theGroup = detail.getParent().getOrderGroup();
			if (theGroup != null)
				groupStr = theGroup.getDomainId();
		}
		setOrderGroupId(groupStr);

		// from v5, housekeeping wi may have no detail
		String orderStr = "";
		if (detail != null)
			orderStr = detail.getOrderId();
		setOrderId(orderStr);

		setContainerId(inWi.getContainerId());
		setItemId(inWi.getItemId());
		setUom(inWi.getUomMasterId());
		setLotId("");

		// Use the denormalized version on the work instruction. That usually will match the location alias that customer is using.
		// Or, for location based pick, will match the location in the order detail
		String locationStr = inWi.getPickInstruction();
		setLocationId(locationStr);

		String pickerId = inWi.getPickerId(); // this field is nullable on work instruction
		if (pickerId == null)
			pickerId = "";
		setBadge(pickerId);
		
		Worker worker = Worker.findTenantWorker(pickerId); 
		if (worker != null) {
			setWorker(worker.getWorkerNameUI());
		}
		
		if (inWi.getPlanQuantity() != null) {
			setPlanQuantity(String.valueOf(inWi.getPlanQuantity()));
		}
		if (inWi.getActualQuantity() != null) {
			setActualQuantity(String.valueOf(inWi.getActualQuantity()));
		}
		if (detail != null && detail.getQuantity() != null) {
			setDetailQuantity(String.valueOf(detail.getQuantity()));
		}
		
		setCheId(inWi.getAssignedCheName());
		
		TimeZone tz = inWi.getFacility().getTimeZone();
		setAssigned(formatDate(inWi.getAssigned(), tz));
		setStarted(formatDate(inWi.getStarted(), tz));
		setCompleted(formatDate(inWi.getCompleted(), tz));
	}
	
	private String formatDate(Timestamp time, TimeZone tz) {
		if (time == null) {
			return "";
		} else {
			synchronized(timestampFormatter) {
				timestampFormatter.setTimeZone(tz);
				return timestampFormatter.format(time);
			}
		}
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
