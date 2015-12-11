package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.Validatable;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.TimeUtils;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "worker", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Worker extends DomainObjectTreeABC<Facility> implements Validatable {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Worker.class);
	
	public static class WorkerDao extends GenericDaoABC<Worker> implements ITypedDao<Worker> {
		public final Class<Worker> getDaoClass() {
			return Worker.class;
		}
	}

	public static ITypedDao<Worker> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Worker.class);
	}

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean		active;

	@Column(nullable = true, name = "first_name")
	@Getter
	@Setter
	@JsonProperty
	private String		firstName;

	@Column(nullable = false, name = "last_name")
	@Getter
	@Setter
	@JsonProperty
	private String		lastName;

	@Column(nullable = true, name = "middle_initial")
	@Getter
	@Setter
	@JsonProperty
	private String		middleInitial;

	@Column(nullable = true, name = "group_name")
	@Getter
	@Setter
	@JsonProperty
	private String		groupName;

	@Column(nullable = true, name = "hr_id")
	@Getter
	@Setter
	@JsonProperty
	private String		hrId;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp	updated;
	
	@Column(nullable = true, name = "last_login")
	@Getter
	@Setter
	@JsonProperty
	private Timestamp	lastLogin;

	@Column(nullable = true, name = "last_logout")
	@Getter
	@Setter
	@JsonProperty
	private Timestamp	lastLogout;
	
	@Column(nullable = true, name = "last_che_persistent_id")
	@Type(type="com.codeshelf.persistence.DialectUUIDType")
	@Getter 
	@Setter
	@JsonProperty
	private UUID 		lastChePersistentId;
	
	@OneToMany(mappedBy = "parent", orphanRemoval = true)
	private List<WorkerHourlyMetric>	workerMetrics;

	@Override
	public String getDefaultDomainIdPrefix() {
		return "W";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<Worker> getDao() {
		return staticGetDao();
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}
	
	@Override
	public boolean isValid(ErrorResponse errors) {
		boolean allOK = true;
		if (lastName == null || "".equals(lastName)) {
			errors.addErrorMissingBodyParam("lastName");
			allOK = false;
		}
		if (getDomainId() == null || "".equals(getDomainId())) {
			errors.addErrorMissingBodyParam("domainId");
			allOK = false;
		}
		if (active == null) {
			errors.addErrorMissingBodyParam("active");
			allOK = false;
		}
		return allOK;
	}

	public void update(Worker updatedWorker) {
		setDomainId(updatedWorker.getDomainId());
		firstName = updatedWorker.getFirstName();
		lastName = updatedWorker.getLastName();
		middleInitial = updatedWorker.getMiddleInitial();
		active = updatedWorker.getActive();
		groupName = updatedWorker.getGroupName();
		hrId = updatedWorker.getHrId();
		updated = new Timestamp(System.currentTimeMillis());
	}

	/**
	 * Find the worker with this badge ID across this tenant
	 */
	public static Worker findTenantWorker(String badgeId) {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("domainId", badgeId));
		List<Worker> workers = staticGetDao().findByFilter(filterParams);
		if (workers == null || workers.isEmpty()) {
			return null;
		}
		if (workers.size() > 1)
			LOGGER.error("More than one worker with badge {} found in findTenantWorker", badgeId);
		return workers.get(0);
	}

	public static Worker findWorker(Facility facility, String badgeId) {
		return findWorker(facility, badgeId, null);
	}

	//TODO now that badges are unique in Tenant, do we still need 'skipWorker' field
	public static Worker findWorker(Facility facility, String badgeId, UUID skipWorker) {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("parent", facility));
		filterParams.add(Restrictions.eq("domainId", badgeId));
		filterParams.add(Restrictions.eq("active", true));
		if (skipWorker != null) {
			//Ignore provided Worker when needed
			filterParams.add(Restrictions.ne("persistentId", skipWorker));
		}
		List<Worker> workers = staticGetDao().findByFilter(filterParams);
		if (workers == null || workers.isEmpty()) {
			return null;
		}
		if (workers.size() > 1)
			LOGGER.error("More than one worker with badge {} found in findWorker", badgeId);
		return workers.get(0);
	}

	public String getWorkerNameUI() {
		if (firstName != null && !firstName.isEmpty()) {
			return firstName + " " + lastName;
		}
		if (!lastName.isEmpty()) {
			return lastName;
		}
		return getDomainId();
	}

	public WorkerHourlyMetric getHourlyMetric(Timestamp timestamp){
		//If this function is hurting performance, it can be changed to make a direct DB call for the metric object. 
		long requestedTime = timestamp.getTime();
		if (workerMetrics != null) {
			for (WorkerHourlyMetric metric : workerMetrics){
				long foundTime = metric.getHourTimestamp().getTime();
				if (requestedTime >= foundTime && requestedTime < foundTime + TimeUtils.MILLISECOUNDS_IN_HOUR){
					return metric;
				}
			}
		}
		return null;
	}
	
	public WorkerHourlyMetric getLatestActiveHourlyMetricBeforeTime(Timestamp timestamp){
		Timestamp latestTime = new Timestamp(0);
		WorkerHourlyMetric latestMetric = null;
		if (workerMetrics != null){
			for (WorkerHourlyMetric metric : workerMetrics){
				Timestamp foundTime = metric.getHourTimestamp();
				if (metric.isSessionActive() && foundTime.before(timestamp) && foundTime.after(latestTime)){
					latestTime = foundTime;
					latestMetric = metric;
				}
			}
		}
		return latestMetric;
	}	
}