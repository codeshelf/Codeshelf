package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "property_default")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class DomainObjectPropertyDefault {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER = LoggerFactory.getLogger(DomainObjectPropertyDefault.class);
	
	public DomainObjectPropertyDefault() {
	}
	
	public DomainObjectPropertyDefault(String name, String objectType, String defaultValue, String description) {
		this.name = name;
		this.objectType = objectType;
		this.defaultValue = defaultValue;
		this.description = description;
	}
	
	@Id
	@NonNull
	@Column(name = "persistentid", nullable = false)
	@Getter
	@JsonProperty
	@Type(type="com.codeshelf.platform.persistence.DialectUUIDType")
	private UUID persistentId = UUID.randomUUID();

	@Getter
	@Column(length=40, nullable=false)
	String name;
	
	@Getter
	@Column(name="object_type",length=40, nullable=false)
	String objectType;
	
	@Getter @Setter
	@Column(name="default_value",length=120, nullable=false)
	String defaultValue;
	
	@Getter @Setter
	@Column(length=400, nullable=false)
	String description;
	
	@Getter
	@OneToMany(mappedBy = "propertyDefault")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<DomainObjectProperty> properties = new ArrayList<DomainObjectProperty>();
	
	public int getIntValue() {
		if (this.defaultValue == null) {
			return Integer.parseInt(getDefaultValue());
		}
		return Integer.parseInt(this.defaultValue);
	}

	public double getDoubleValue() {
		if (this.defaultValue == null) {
			return Double.parseDouble(getDefaultValue());
		}
		return Double.parseDouble(this.defaultValue);
	}

	public boolean getBooleanValue() {
		if (this.defaultValue == null) {
			return Boolean.parseBoolean(getDefaultValue());
		}
		return Boolean.parseBoolean(this.defaultValue);
	}	
}
