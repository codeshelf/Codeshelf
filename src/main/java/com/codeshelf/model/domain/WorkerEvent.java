package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.UUID;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

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
		LOGIN, LOGOUT, SKIP_ITEM_SCAN, BUTTON, WI, SHORT, SHORT_AHEAD, COMPLETE, CANCEL_PUT, DETAIL_WI_MISMATCHED, PALLETIZER_PUT, PUTWALL_PUT, SKUWALL_PUT, LOW;
		
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
	private Timestamp created;

	@Column(nullable = false, name = "event_type")
	@Enumerated(EnumType.STRING)
	@Getter @Setter
	@JsonProperty
	private WorkerEvent.EventType						eventType;

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
	@Getter @Setter
	private UUID							orderDetailId;

	@Column(nullable = true, name = "work_instruction_persistentid", insertable=false, updatable=false)
	@Type(type="com.codeshelf.persistence.DialectUUIDType")
	@Getter @Setter
	private UUID 		workInstructionId;

	@Column(nullable = true, name = "description")
	@Getter
	@JsonProperty
	private String							description;
	
	@Column(nullable = true)
	@Getter @Setter
	@JsonProperty
	private String							location;


	public WorkerEvent() {
		setCreated(new Timestamp(System.currentTimeMillis()));
	}

	public WorkerEvent(DateTime created, WorkerEvent.EventType eventType, Che che, String workerId) {
		setCreated(new Timestamp(created.getMillis()));
		setDeviceGuid(che.getDeviceGuidStr());
		setDevicePersistentId(che.getPersistentId().toString());
		setEventType(eventType);
		setParent(che.getFacility());
		setWorkerId(workerId);
		generateDomainId();
	}

	public WorkerEvent(WorkerEvent.EventType eventType, Facility facility, String workerId, String description) {
		setCreated(new Timestamp(System.currentTimeMillis()));
		setEventType(eventType);
		setParent(facility);
		setWorkerId(workerId);
		setDevicePersistentId("");
		setDeviceGuid("");
		setDescription(description);
		generateDomainId();
	}
	
	@Override
	public Facility getFacility() {
		return getParent();
	}
	
	public void setDescription(String description){
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
}
