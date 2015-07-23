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

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "event_worker")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class WorkerEvent extends DomainObjectABC {
	public static class WorkerEventDao extends GenericDaoABC<WorkerEvent> implements ITypedDao<WorkerEvent> {
		public final Class<WorkerEvent> getDaoClass() {
			return WorkerEvent.class;
		}
	}

	public enum EventType {
		LOGIN, SKIP_ITEM_SCAN, BUTTON, WI, SHORT, SHORT_AHEAD, COMPLETE, CANCEL_PUT, DETAIL_WI_MISMATCHED;
		
		public String getName() {
			return name();
		}
	}

	public static ITypedDao<WorkerEvent> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(WorkerEvent.class);
	}

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Setter
	private Facility						facility;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "work_instruction_persistentid", foreignKey=@ForeignKey(name="none", value=ConstraintMode.NO_CONSTRAINT ))
	@Type(type="com.codeshelf.persistence.DialectUUIDType")
	@Getter @Setter
	private WorkInstruction							workInstruction;

	@Column(nullable = true, name = "work_instruction_persistentid", insertable=false, updatable=false)
	@Type(type="com.codeshelf.persistence.DialectUUIDType")
	@Getter @Setter
	private UUID 		workInstructionId;

	@Column(nullable = true, name = "description")
	@Getter
	@JsonProperty
	private String							description;

	public WorkerEvent() {
		setCreated(new Timestamp(System.currentTimeMillis()));
	}

	public WorkerEvent(DateTime created, WorkerEvent.EventType eventType, Che che, String workerId) {
		setCreated(new Timestamp(created.getMillis()));
		setDeviceGuid(che.getDeviceGuidStr());
		setDevicePersistentId(che.getPersistentId().toString());
		setEventType(eventType);
		setFacility(che.getFacility());
		setWorkerId(workerId);
		generateDomainId();
	}

	public WorkerEvent(WorkerEvent.EventType eventType, Facility facility, String workerId, String description) {
		setCreated(new Timestamp(System.currentTimeMillis()));
		setEventType(eventType);
		setFacility(facility);
		setWorkerId(workerId);
		setDevicePersistentId("");
		setDeviceGuid("");
		setDescription(description);
		generateDomainId();
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

	@Override
	public Facility getFacility() {
		return facility;
	}

	public void generateDomainId(){
		String domainId = getDefaultDomainIdPrefix() + "_" + getDeviceGuid() + "_" + getEventType() + "_" + getCreated().getTime();
		setDomainId(domainId);
	}
}
