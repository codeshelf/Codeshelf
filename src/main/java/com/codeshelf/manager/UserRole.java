package com.codeshelf.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "user_role") // role is reserved word
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
//@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
//@JsonIgnoreProperties({"className"})
@EqualsAndHashCode(of={"name"})
public class UserRole {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER					= LoggerFactory.getLogger(UserRole.class);

	public static final String	TOKEN_SEPARATOR	= ",";

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
	//@JsonProperty
	Date created;
	//
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false,name="last_modified")
	//@JsonProperty
	Date lastModified;
	//
	@PrePersist
	protected void onCreate() { this.created = this.lastModified = new Date(); }
	@PreUpdate
	protected void onUpdate() { this.lastModified = new Date(); }
	/* Timestamped entity */

	@Getter
	@Column(nullable = false, name="name", unique=true)
	@JsonProperty
	@NaturalId
	String name;

	@ManyToMany(targetEntity=UserPermission.class, fetch=FetchType.EAGER)
	@JoinTable(name="roles_permissions")
	@Getter
	@JsonProperty
	Set<UserPermission> permissions = new HashSet<UserPermission>();
		
	public void setName(String newName) {
		if(nameIsValid(newName)) {
			this.name = newName;
		} else {			
			throw new RuntimeException("Role cannot contain comma");
		}
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static boolean nameIsValid(String name) {
		if(name==null)
			return false;
		return !name.contains(TOKEN_SEPARATOR);
	}
	
	public Collection<String> getPermissionStrings() {
		Collection<String> permissionStrings = new ArrayList<String>(this.getPermissions().size());
		for(UserPermission perm : this.getPermissions()) {
			permissionStrings.add(perm.getDescriptor());
		}
		return permissionStrings;
	}

	public void setPermissions(Set<UserPermission> permissions) {
		if(!this.permissions.equals(permissions)) {
			this.permissions.clear();
			this.permissions.addAll(permissions);
		}
	}
}
