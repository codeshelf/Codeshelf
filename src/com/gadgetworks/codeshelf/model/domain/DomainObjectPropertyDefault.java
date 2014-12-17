package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;

import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "property_default")
public class DomainObjectPropertyDefault {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(DomainObjectPropertyDefault.class);
	
	public DomainObjectPropertyDefault() {
	}
	
	// construction should only be used for unit tests.  domain object property meta data aka config
	// types are read-only and initialized via liquibase.
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
	@Type(type="com.gadgetworks.codeshelf.platform.persistence.DialectUUIDType")
	private UUID persistentId = UUID.randomUUID();

	@Getter
	@Column(nullable=false)
	String name;
	
	@Getter
	@Column(nullable=false)
	String objectType;
	
	@Getter
	@Column(nullable=false)
	String defaultValue;
	
	@Getter
	@Column(nullable=false)
	String description;	
}
