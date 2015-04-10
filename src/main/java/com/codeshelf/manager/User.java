/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: User.java,v 1.23 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.security.UserContext;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@JsonIgnoreProperties({"className","tenantId"})
@EqualsAndHashCode(of={"username"})
public class User implements UserContext {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER					= LoggerFactory.getLogger(User.class);

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
	@JsonIgnore
	Date created;
	//
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false,name="last_modified")
	@JsonIgnore
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
	@JsonIgnore
	private Tenant				tenant;
	
	@Column(nullable = false,name="username")
	@NonNull
	@Getter
	@Setter
	@JsonProperty
	@NaturalId
	private String				username;

	// The hashed password including salt in APR1 format (NGINX/HTTPD compatible)
	@Column(name="hashed_password")
	@Getter
	@Setter
	//not JSON
	private String				hashedPassword;

	// Is it active.
	@Getter
	@Setter
	@Column(nullable = false,name="active")
	@JsonProperty
	private boolean				active;
	
	@Transient
	private Set<String>			permissionsFromDeserialization = null;
	
	// roles assigned to user
	@ManyToMany(targetEntity=UserRole.class, fetch=FetchType.EAGER)
	@JoinTable(name="users_roles")
	@Getter
	Set<UserRole> roles = new HashSet<UserRole>();

	public User() {
		active = true;
	}
	
	@JsonIgnore
	protected String getHtpasswdEntry() {
		return this.getUsername()+":"+this.hashedPassword;
	}

	@JsonIgnore
	public boolean isLoginAllowed() {
		// TODO: also check user flag 
		return this.isActive();
	}
	
	@Override
	public String toString() {
		return this.getUsername();
	}
	
	public void setRoles(Set<UserRole> newRoles) {
		if(!this.roles.equals(newRoles)) {
			this.roles.clear();
			this.roles.addAll(newRoles);
		}
	}
	
	@Override
	@JsonProperty("roles")
	public Collection<String> getRoleNames() {
		List<String> roleNames = new ArrayList<String>(this.getRoles().size());
		for(UserRole role : this.getRoles()) {
			roleNames.add(role.getName());
		}
		return roleNames;
	}
	
	@Override
	@JsonProperty("permissions")
	public Set<String> getPermissionStrings() {
		if(this.permissionsFromDeserialization != null)
			return this.permissionsFromDeserialization;
		
		Set<String> permissionStrings = new HashSet<String>();
		for(UserRole role : this.getRoles()) {
			permissionStrings.addAll(role.getPermissionStrings());
		}
		return permissionStrings;
	}
	
	@JsonProperty("permissions")
	public void setPermissionsFromDeserialization(Set<String> permissionStrings) {
		this.permissionsFromDeserialization = permissionStrings;
	}

	@JsonIgnore
	public Set<UserPermission> getPermissions() {
		Set<UserPermission> permissions = new HashSet<UserPermission>();
		for(UserRole role : this.getRoles()) {
			permissions.addAll(role.getPermissions());
		}
		return permissions;
	}
	
	@Override
	@JsonIgnore
	public boolean isSiteController() {
		return this.getRoleNames().contains(DefaultRolesPermissions.SITE_CONTROLLER_ROLE);
	}
	
	@JsonProperty("tenantId")
	public int getTenantId() {
		return this.getTenant().getId();
	}	
}
