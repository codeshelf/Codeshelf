package com.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.edi.WorkInstructionCsvBean;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wi_bean")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class WIBeanDBStorage extends DomainObjectTreeABC<Facility>{
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter @Setter
	protected Facility		parent;

	@Column(nullable = false, columnDefinition = "TEXT")
	@Getter @Setter
	@JsonProperty
	String 					bean;
	
	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private Boolean			active;
	
	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private Timestamp 		updated;

	public static class WIBeanDBStorageDao extends GenericDaoABC<WIBeanDBStorage> implements ITypedDao<WIBeanDBStorage> {
		public final Class<WIBeanDBStorage> getDaoClass() {
			return WIBeanDBStorage.class;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public final ITypedDao<WIBeanDBStorage> getDao() {
		return staticGetDao();
	}


	public static ITypedDao<WIBeanDBStorage> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(WIBeanDBStorage.class);
	}
	
	public WIBeanDBStorage() {
		// TODO Auto-generated constructor stub
	}
	
	public WIBeanDBStorage(WorkInstructionCsvBean bean) {
		setParent(bean.getFacility());
		setActive(true);
		setDomainId(getDefaultDomainIdPrefix() + "_" + System.currentTimeMillis());
		setUpdated(new Timestamp(System.currentTimeMillis()));
		setBean(bean.toString());
	}


	@Override
	public String getDefaultDomainIdPrefix() {
		return "WIBean";
	}


	@Override
	public Facility getFacility() {
		return getParent();
	}
}