/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: User.java,v 1.23 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.manager;

import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.codec.digest.Md5Crypt;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.UserType;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// --------------------------------------------------------------------------
/**
 * User
 * 
 * This holds all of the information about limited-time use codes we send to prospects.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "users") // user can be reserved word in sql
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@JsonIgnoreProperties({"className"})
@EqualsAndHashCode(of={"username","type"})
@ToString(of={"username"}, callSuper = false)
public class User {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER				= LoggerFactory.getLogger(User.class);

	@Id
	@Column(nullable = false,name="id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Getter
	@Setter
	@JsonProperty
	Integer id;
	
	/* Timestamped entity */
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false,name="created")
	@JsonProperty
	Date created;
	//
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false,name="last_modified")
	@JsonProperty
	Date lastModified;
	//
	@PrePersist
	protected void onCreate() { this.created = this.lastModified = new Date(); }
	@PreUpdate
	protected void onUpdate() { this.lastModified = new Date(); }
	/* Timestamped entity */

	// The owning organization.
	@ManyToOne(optional = false,fetch=FetchType.EAGER)
	@Getter
	@Setter
	@JsonProperty
	private Tenant				tenant;
	
	@Column(nullable = false,name="username")
	@NonNull
	@Getter
	@Setter
	@JsonProperty
	private String				username;

	// The hashed password including salt in APR1 format (NGINX/HTTPD compatible)
	@Column(name="hashed_password")
	private String				hashedPassword;

	// sitecon, webapp, system user etc
	@Column(nullable = false)
	@Getter
	@Setter
	@Enumerated(value = EnumType.STRING)
	@JsonProperty
	private UserType			type;

	// Is it active.
	@Getter
	@Setter
	@Column(nullable = false,name="active")
	@JsonProperty
	private boolean				active;

	public User() {
		active = true;
	}

	public void setPassword(final String inPassword) {
		hashedPassword = Md5Crypt.apr1Crypt(inPassword);
	}
	
	public boolean checkPassword(final String inPassword) {
		return Md5Crypt.apr1Crypt(inPassword,this.hashedPassword).equals(this.hashedPassword);
	}

	public boolean tenantEquals(Tenant tenant2) {
		return this.getTenant().equals(tenant2);
	}
	
	public static boolean passwordMeetsRequirements(String password) {
		if(password == null) 
			return false;
		if(password.isEmpty())
			return false;
		
		return true;
	}
	
	public String getHtpasswdEntry() {
		return this.getUsername()+":"+this.hashedPassword;
	}
	
	public boolean hashIsValid() {
		return this.hashedPassword.startsWith("$apr1$");
	}
}
