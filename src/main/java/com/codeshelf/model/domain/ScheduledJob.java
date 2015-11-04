package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.text.ParseException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.quartz.CronExpression;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.scheduler.ScheduledJobType;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scheduled_job")
public class ScheduledJob extends DomainObjectTreeABC<Facility> {

	// The parent facility.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	@Setter
	private Facility					parent;

	@Column(nullable = false, name = "type")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private ScheduledJobType 			type;
	
	@Column(nullable = false, name="cron_expression")
	@Getter
	@Setter
	@JsonProperty
	private String 				cronExpression;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String 						name;
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean						active;
	
	@Column(nullable = true)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp					updated;
	
	public ScheduledJob() {
		super();
	}

	
	public ScheduledJob(Facility facility, ScheduledJobType type, String cronExpression) throws ParseException {
		this.setParent(facility);
		this.setDomainId(type.name());
		this.setName(type.name());
		this.setType(type);
		this.setCronExpression(new CronExpression(cronExpression).getCronExpression());
	}
	
	@Override
	public String getDefaultDomainIdPrefix() {
		// TODO Auto-generated method stub
		return "job";
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<ScheduledJob> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<ScheduledJob> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(ScheduledJob.class);
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}

}
