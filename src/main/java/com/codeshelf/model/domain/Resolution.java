package com.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "resolution")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Resolution extends DomainObjectTreeABC<Facility> {
	public static class ResolutionDao extends GenericDaoABC<Resolution> implements ITypedDao<Resolution> {
		public final Class<Resolution> getDaoClass() {
			return Resolution.class;
		}
	}
	
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@Getter
	private Facility						facility;

	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private Timestamp						timestamp;

	@Column(nullable = true, name = "resolved_by")
	@Getter @Setter
	@JsonProperty
	private String							resolvedBy;
	
	public static ITypedDao<Resolution> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Resolution.class);
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "R";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<Resolution> getDao() {
		return staticGetDao();
	}

	@Override
	public Facility getParent() {
		return getFacility();
	}

	@Override
	public void setParent(Facility inParent) {
		this.facility = inParent;
	}
}
