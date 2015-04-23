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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.service.NotificationService.EventType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "event_worker")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Event extends DomainObjectABC {
	public static class EventDao extends GenericDaoABC<Event> implements ITypedDao<Event> {
		public final Class<Event> getDaoClass() {
			return Event.class;
		}
	}
	
	public static ITypedDao<Event> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Event.class);
	}

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Setter
	private Facility						facility;

	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private Timestamp						created;

	@Column(nullable = false, name = "event_type")
	@Enumerated(EnumType.STRING)
	@Getter @Setter
	@JsonProperty
	private EventType						eventType;
	
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
	private UUID						orderDetailId;

	@Column(nullable = true, name = "work_instruction_persistentid")
	@Type(type="com.codeshelf.persistence.DialectUUIDType")
	@Getter @Setter
	private UUID						workInstructionId;

	public Event() {
		setCreated(new Timestamp(System.currentTimeMillis()));
	}
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "Ev";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<Event> getDao() {
		return staticGetDao();
	}

	@Override
	public Facility getFacility() {
		return facility;
	}
	
	public void generateDomainId(){
		String domainId = getDefaultDomainIdPrefix() + "_" + getDeviceGuid() + "_" + getEventType();
		setDomainId(domainId);
	}
}
