package com.codeshelf.manager;

import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.apache.shiro.authz.Permission;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "permission") 
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
//@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
//@JsonIgnoreProperties({"className"})
@EqualsAndHashCode(of={"descriptor"})
public class UserPermission implements Permission {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER					= LoggerFactory.getLogger(UserPermission.class);

	public static final String	TOKEN_SEPARATOR	= ","; // this is the token separating whole permission descriptors

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
	@Setter
	@Column(nullable = false, name="descriptor", unique=true)
	@JsonProperty
	String descriptor;

	@Override
	public boolean implies(Permission p) {
		return false;
	}
	@Override
	public String toString() {
		return descriptor;
	}
	public static boolean descriptorIsValid(String descriptor) {
		if(descriptor==null)
			return false;
		return !descriptor.contains(TOKEN_SEPARATOR);
	}

}
