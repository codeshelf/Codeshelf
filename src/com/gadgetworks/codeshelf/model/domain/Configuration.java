package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "configuration")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Configuration {
	
	@Id
	@NonNull
	@Column(name = "persistentid", nullable = false)
	@Getter
	@Setter
	@JsonProperty
	@Type(type="com.gadgetworks.codeshelf.platform.persistence.DialectUUIDType")
	private UUID persistentId = UUID.randomUUID();

	@Version
	@Column(nullable = false)
	@Getter
	@Setter
	// @JsonProperty do we need to serialize this?
	private long version;

	@Getter
	@NonNull
	@Column(name = "objectid", nullable = false)
	@Type(type="com.gadgetworks.codeshelf.platform.persistence.DialectUUIDType")
	private UUID objectId = null;

	@Getter
	@Column(length=20, nullable=false)
	String objectType;

	@Getter
	@Column(length=50, nullable=false)
	String name;
	
	@Getter @Setter
	@Column(length=200, nullable=false)
	String value;
	
	@Getter @Setter
	@Column(length=120, nullable=true)
	String description;
	
	public Configuration() {
	}
	
	public Configuration(IDomainObject object, String name) {
		this.objectType = object.getClassName();
		this.objectId = object.getPersistentId();
		this.name = name;
	}
	
	public Configuration(IDomainObject object, String name, String value) {
		this(object,name);
		this.value = value;
	}

	public Configuration(IDomainObject object, String name, String value, String description) {
		this(object,name,value);
		this.description = description;
	}
	
	public Configuration setValue(int intValue) {
		this.value = Integer.toString(intValue);
		return this;
	}
	
	public Configuration setValue(double doubleValue) {
		this.value = Double.toString(doubleValue);
		return this;
	}
}
