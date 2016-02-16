package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.UUID;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import com.codeshelf.model.ReplenishItem;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "event_worker", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class WorkerEvent extends DomainObjectTreeABC<Facility> {
	public static class WorkerEventDao extends GenericDaoABC<WorkerEvent> implements ITypedDao<WorkerEvent> {
		public final Class<WorkerEvent> getDaoClass() {
			return WorkerEvent.class;
		}
	}

	public enum EventType {
		LOGIN,
		LOGOUT,
		SKIP_ITEM_SCAN,
		BUTTON,
		SHORT,
		SHORT_AHEAD,
		COMPLETE,
		CANCEL_PUT,
		DETAIL_WI_MISMATCHED,
		LOW,
		SUBSTITUTION;
		
		public String getName() {
			return name();
		}
	}

	public static ITypedDao<WorkerEvent> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(WorkerEvent.class);
	}

	@ManyToOne(optional = true, fetch = FetchType.LAZY)
	@Getter @Setter
	private Resolution						resolution;

	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private Timestamp 						created;

	@Column(nullable = false, name = "event_type")
	@Enumerated(EnumType.STRING)
	@Getter @Setter
	@JsonProperty
	private WorkerEvent.EventType			eventType;

	@Column(nullable = false, name = "device_persistentid")
	@Getter @Setter
	@JsonProperty
	private String							devicePersistentId;

	@Column(nullable = false, name = "device_guid")
	@Getter @Setter
	@JsonProperty
	private String							deviceGuid;

	@Column(nullable = true, name = "worker_id")
	@Getter @Setter
	@JsonProperty
	private String							workerId;

	@Column(nullable = true, name = "order_detail_persistentid")
	@Type(type="com.codeshelf.persistence.DialectUUIDType")
	@Getter @Setter(AccessLevel.PRIVATE)
	private UUID							orderDetailId;

	@Column(nullable = true, name = "work_instruction_persistentid")
	@Type(type="com.codeshelf.persistence.DialectUUIDType")
	@Getter
	private UUID 							workInstructionId;

	@Column(nullable = true, name="item_id")
	@Getter @Setter(AccessLevel.PRIVATE)
	@JsonProperty
	private String							itemId;

	@Column(nullable = true, name="item_gtin")
	@Getter @Setter(AccessLevel.PRIVATE)
	@JsonProperty
	private String							itemGtin;

	@Column(nullable = true, name="item_uom")
	@Getter @Setter(AccessLevel.PRIVATE)
	@JsonProperty
	private String							itemUom;
	
	
	@Column(nullable = true, name="item_description")
	@Getter @Setter(AccessLevel.PRIVATE)
	@JsonProperty
	private String							itemDescription;
	
	@Column(nullable = true, name = "description")
	@Getter
	@JsonProperty
	private String							description;
	
	@Column(nullable = true)
	@Getter @Setter
	@JsonProperty
	private String							location;

	@Column(nullable = true)
	@Getter @Setter
	@JsonProperty
	private String							purpose;
	
	@Column(nullable = true, name = "path_name")
	@Getter @Setter
	@JsonProperty
	private String							pathName;

	public WorkerEvent() {
		setCreated(new Timestamp(System.currentTimeMillis()));
	}

	public WorkerEvent(DateTime created, WorkerEvent.EventType eventType, Che che, String workerId) {
		setParent(che.getFacility());
		setDeviceGuid(che.getDeviceGuidStr());
		setDevicePersistentId(che.getPersistentId().toString());
		setCreated(new Timestamp(created.getMillis()));
		setEventType(eventType);
		setWorkerId(workerId);
		generateDomainId();
	}

	public WorkerEvent(WorkerEvent.EventType eventType, Facility facility, String workerId, String description) {
		setParent(facility);
		setDevicePersistentId("");
		setDeviceGuid("");
		setCreated(new Timestamp(System.currentTimeMillis()));
		setEventType(eventType);
		setWorkerId(workerId);
		setDescription(description);
		generateDomainId();
	}
	
	@Override
	public Facility getFacility() {
		return getParent();
	}
	
	private void setDescription(String description){
		if (description != null && description.length() > 255){
			description = description.substring(0, 255);
		}
		this.description = description;
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "Ev";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<WorkerEvent> getDao() {
		return staticGetDao();
	}

	public void generateDomainId(){
		String domainId = getDefaultDomainIdPrefix() + "_" + getDeviceGuid() + "_" + getEventType() + "_" + getCreated().getTime();
		setDomainId(domainId);
	}
	
	public String toString() {
		return String.format("%s(facility: %s, eventType: %s, persistentId: %s", this.getClass().getSimpleName(), getParent(), eventType, getPersistentId());
	}
	
	public void setWorkInstruction(WorkInstruction wi) {
		workInstructionId = wi.getPersistentId();
		setPathName(wi.getPathName());
		setPurpose(wi.getPurpose().name());
		setCreated(wi.getCompleted());
		setWorkerId(wi.getPickerId());
		setLocation(wi.getPickInstruction());
		setItemId(wi.getItemId());
		setItemGtin(wi.getGtin());
		setItemUom(wi.getUomMasterId());
		setItemDescription(wi.getDescription());
		OrderDetail orderDetail = wi.getOrderDetail();
		if (orderDetail != null) {
			setOrderDetailId(orderDetail.getPersistentId());
		}
	}

	public ReplenishItem toReplenishItem() {
		 ReplenishItem replenItem = new ReplenishItem();
		 replenItem.setItemId(this.getItemId());
		 replenItem.setGtin(this.getItemGtin());
		 replenItem.setUom(this.getItemUom());
		 replenItem.setLocation(this.getLocation());
		 return replenItem;		
		}
}
