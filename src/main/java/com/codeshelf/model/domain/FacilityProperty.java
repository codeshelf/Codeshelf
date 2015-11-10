package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facility_property")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class FacilityProperty extends DomainObjectTreeABC<Facility>{
	public static class FacilityPropertyDao extends GenericDaoABC<FacilityProperty> implements ITypedDao<FacilityProperty> {
		public final Class<FacilityProperty> getDaoClass() {
			return FacilityProperty.class;
		}
	}
	
	@ManyToOne(optional = false, fetch=FetchType.EAGER)
	@Getter
	@Setter
	private Facility	parent;
	
	@Column(nullable = false)
	@Getter
	@JsonProperty
	private String	name;
	
	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private String	value;
	
	@Column(nullable = false)
	@Getter @Setter
	@JsonProperty
	private String	description;
	
	@Transient
	@Getter @Setter
	@JsonProperty
	private String defaultValue;

	public FacilityProperty() {
	}
	
	public FacilityProperty(Facility facility, FacilityPropertyType type){
		parent = facility;
		name = type.name();
		value = type.getDefaultValue();
		description = type.getDescription();
		setDomainId(getDefaultDomainIdPrefix() + "_" + name); 
	}
	
	@Override
	public String getDefaultDomainIdPrefix() {
		return "FP";
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ITypedDao<FacilityProperty> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<FacilityProperty> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(FacilityProperty.class);
	}

	@Override
	public Facility getFacility() {
		return parent;
	}
	
	public void updateDefaultValue(){
		FacilityPropertyType type = FacilityPropertyType.valueOf(getName());
		setDefaultValue(type == null ? "No Default Value Found" : type.getDefaultValue());
	}
}