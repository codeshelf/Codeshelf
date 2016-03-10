package com.codeshelf.model.domain;

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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "print_template", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class PrintTemplate extends DomainObjectTreeABC<Facility> {

	public static class PrintTemplateDao extends GenericDaoABC<PrintTemplate> implements ITypedDao<PrintTemplate> {
		public final Class<PrintTemplate> getDaoClass() {
			return PrintTemplate.class;
		}
	}

	public static ITypedDao<PrintTemplate> staticGetaaDao() {
		return TenantPersistenceService.getInstance().getDao(PrintTemplate.class);
	}

	
	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private boolean	active=true;

	@Column(nullable = false, columnDefinition = "TEXT")
	@Getter @Setter
	@JsonProperty
	String template;

	public PrintTemplate() {
		super();
	}

	public PrintTemplate(Facility facility, String domainId) {
		super(domainId);
		setParent(facility);
		setActive(false);
		setTemplate(getDefaultTemplate());
	}

	private String getDefaultTemplate() {
		return "";
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "";
	}

	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<PrintTemplate> getDao() {
		// TODO Auto-generated method stub
		return PrintTemplate.staticGetaaDao();
	}

	@Override
	public Facility getFacility() {
		return getParent();
	}

}
