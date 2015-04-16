package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.Validatable;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "worker")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Worker extends DomainObjectABC implements Validatable{

	public static class WorkerDao extends GenericDaoABC<Worker> implements ITypedDao<Worker> {
		public final Class<Worker> getDaoClass() {
			return Worker.class;
		}
	}
	
	public static ITypedDao<Worker> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Worker.class);
	}

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Setter
	private Facility						facility;
	
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean							active;
	
	@Column(nullable = false, name = "first_name")
	@Getter
	@JsonProperty
	private String							firstName;

	@Column(nullable = false, name = "last_name")
	@Getter
	@JsonProperty
	private String							lastName;
	
	@Column(nullable = true, name = "middle_initial")
	@Getter
	@JsonProperty
	private String							middleInitial;

	@Column(nullable = false, name = "badge_id")
	@Getter
	@JsonProperty
	private String							badgeId;

	@Column(nullable = true, name = "group_name")
	@Getter
	@JsonProperty
	private String							groupName;

	@Column(nullable = true, name = "hr_id")
	@Getter
	@JsonProperty
	private String							hrId;
	
	@Column(nullable = false)
	@Setter
	@JsonProperty
	private Timestamp						updated;


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
		return facility;
	}
	
	public void generateDomainId(){
		setDomainId(String.format("%s-%s-%s", getDefaultDomainIdPrefix(), getLastName(), getFirstName()));
	}
	
	@Override
	public boolean isValid(ErrorResponse errors) {
		getDomainId();
		boolean allOK = true;
		if (firstName == null || "".equals(firstName)){
			errors.addErrorMissingBodyParam("firstName");
			allOK = false;
		}
		if (lastName == null || "".equals(lastName)){
			errors.addErrorMissingBodyParam("lastName");
			allOK = false;
		}
		if (badgeId == null || "".equals(badgeId)){
			errors.addErrorMissingBodyParam("badgeId");
			allOK = false;
		}
		if (active == null){
			errors.addErrorMissingBodyParam("active");
			allOK = false;
		} else {
			//If (active == null), the validation will fail. No need to check for badge uniqueness
			if (!isBadgeUnique()) {
				errors.addError("Active worker with badge " + badgeId + " already exists");
				allOK = false;
			}
		}
		return allOK;
	}
	
	public void update(Worker updatedWorker) {
		firstName = updatedWorker.getFirstName();
		lastName = updatedWorker.getLastName();
		middleInitial = updatedWorker.getMiddleInitial();
		active = updatedWorker.getActive();
		badgeId = updatedWorker.getBadgeId();
		groupName = updatedWorker.getGroupName();
		hrId = updatedWorker.getHrId();
		updated = new Timestamp(System.currentTimeMillis());
		generateDomainId();
	}
	
	private boolean isBadgeUnique(){
		//Allow saving inactive workers with non-unique badges
		if (!active) {
			return true;
		}
		//Try to find another active worker with the same badge
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", facility));
		filterParams.add(Restrictions.eq("badgeId", badgeId));
		filterParams.add(Restrictions.eq("active", true));
		//Ignore this Worker itself (if it is already saved in the DB)
		filterParams.add(Restrictions.ne("persistentId", getPersistentId()));
		List<Worker> workers = staticGetDao().findByFilter(filterParams);
		if (workers == null || workers.isEmpty()) {
			return true;
		}
		return false;
	}
}