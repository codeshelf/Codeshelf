package com.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.TimeUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "worker_hourly_metric", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class WorkerHourlyMetric extends DomainObjectTreeABC<Worker>{
	public static class WorkerHourlyMetricDao extends GenericDaoABC<WorkerHourlyMetric> implements ITypedDao<WorkerHourlyMetric> {
		public final Class<WorkerHourlyMetric> getDaoClass() {
			return WorkerHourlyMetric.class;
		}
	}
	
	@Column(nullable = false, name = "hour_timestamp")
	@Getter @Setter	@JsonProperty
	private Timestamp	hourTimestamp;

	@Column(nullable = false, name = "last_session_start")
	@Getter @Setter	@JsonProperty
	private Timestamp	lastSessionStart;
	
	@Column(nullable = false, name = "session_active")
	@Getter @Setter	@JsonProperty
	private boolean		sessionActive = true;

	@Column(nullable = false, name = "loggedin_duration_min")
	@Getter @Setter	@JsonProperty
	private Integer		loggedInDurationMin = 0;
	
	@Column(nullable = false, name = "picks")
	@Getter @Setter	@JsonProperty
	private Integer		picks = 0;
	
	@Column(nullable = false, name = "completes")
	@Getter @Setter	@JsonProperty
	private Integer		completes = 0;
	
	@Column(nullable = false, name = "shorts")
	@Getter @Setter	@JsonProperty
	private Integer		shorts = 0;

	public WorkerHourlyMetric() {
	}
	
	public WorkerHourlyMetric(Worker worker, Timestamp sessionStart) {
		setParent(worker);
		setSessionActive(true);
		setHourTimestamp(TimeUtils.truncateTimeToHour(sessionStart));
		setLastSessionStart(sessionStart);
		setDomainId(getDefaultDomainIdPrefix() + "_" + worker.getDomainId() + "_" + getHourTimestamp());
		WorkerHourlyMetric.staticGetDao().store(this);
	}
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "WHM";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<WorkerHourlyMetric> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<WorkerHourlyMetric> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(WorkerHourlyMetric.class);
	}

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}
}
