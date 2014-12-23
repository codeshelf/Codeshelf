package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.PropertyDao;
import com.google.inject.Inject;

@Entity
@Table(name = "property")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class DomainObjectProperty extends DomainObjectABC implements IDomainObject {
	
	@Inject
	public static ITypedDao<DomainObjectProperty> DAO = PropertyDao.getInstance();

	@Getter
	@NonNull
	@Column(name = "objectid", nullable = false)
	@Type(type="com.gadgetworks.codeshelf.platform.persistence.DialectUUIDType")
	private UUID objectId = null;
	
	@Getter
	@Column(length=120, nullable=false)
	String value;
	
	@Getter @Setter
	@ManyToOne(fetch=FetchType.EAGER,optional=false)
	@JoinColumn(name = "property_default_persistentid")
	DomainObjectPropertyDefault propertyDefault;
	
	public DomainObjectProperty() {
	}
	
	public DomainObjectProperty(IDomainObject object, DomainObjectPropertyDefault propertyDefault) {
		this.setDomainId(propertyDefault.getName());
		this.propertyDefault = propertyDefault;
		this.objectId = object.getPersistentId();
	}
	
	public DomainObjectProperty(IDomainObject object, DomainObjectPropertyDefault type, String value) {
		this(object,type);
		this.value = value;
	}
	
	public DomainObjectProperty setValue(String stringValue) {
		this.value = stringValue;
		return this;
	}
	
	public DomainObjectProperty setValue(int intValue) {
		this.value = Integer.toString(intValue);
		return this;
	}
	
	public DomainObjectProperty setValue(double doubleValue) {
		this.value = Double.toString(doubleValue);
		return this;
	}
	
	public DomainObjectProperty setValue(boolean boolValue) {
		this.value = Boolean.toString(boolValue);
		return this;
	}
	
	public int getIntValue() {		
		if (this.value==null) {
			return Integer.parseInt(getDefaultValue());
		}
		return Integer.parseInt(this.value);
	}
	
	public double getDoubleValue() {		
		if (this.value==null) {
			return Double.parseDouble(getDefaultValue());
		}
		return Double.parseDouble(this.value);
	}
		
	public boolean getBooleanValue() {		
		if (this.value==null) {
			return Boolean.parseBoolean(getDefaultValue());
		}
		return Boolean.parseBoolean(this.value);
	}
		
	// convenience function to get the property name via default/type object
	public String getName() {
		if (propertyDefault==null) {
			return null;
		}
		return propertyDefault.getName();
	}

	// convenience function
	public String getDescription() {
		if (propertyDefault==null) {
			return null;
		}
		return propertyDefault.getDescription();
	}

	// convenience function
	public String getObjectType() {
		if (propertyDefault==null) {
			return null;
		}
		return propertyDefault.getObjectType();
	}

	// convenience function to get the default value via default/type object
	public String getDefaultValue() {
		if (propertyDefault==null) {
			return null;
		}
		return propertyDefault.getDefaultValue();
	}

	@Override
	public String getDefaultDomainIdPrefix() {
		return "PROP";
	}
	
	@Override
	public Facility getFacility() {
		throw new RuntimeException("Not Supported");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ITypedDao<DomainObjectProperty> getDao() {
		return DomainObjectProperty.DAO;
	}

}
