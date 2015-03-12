package com.codeshelf.manager;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.platform.persistence.EventListenerIntegrator;
import com.codeshelf.platform.persistence.Schema;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Entity
@Table(name="tenant")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@JsonIgnoreProperties({"className"})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@EqualsAndHashCode(callSuper = false, of={"name","schemaName","username"})
@ToString(of={"id","name","shard","schemaName"},callSuper=false)
public class Tenant extends Schema {
	private static final String TENANT_CHANGELOG_FILENAME= "liquibase/db.changelog-master.xml";

	public static final int SCHEMA_NAME_MAX_LENGTH = 16;
	private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[a-z0-9]{1,16}$");

	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Tenant.class);

	@Id
	@Column(nullable = false,name="id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Getter
	@Setter
	@JsonProperty
	int id;

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
	
	@Getter 
	@Setter
	@NonNull
	@Column(unique=true, nullable=false,length=255,name="name")
	@JsonProperty
	String name;

	@Getter
	@Setter
	@NonNull
	@Column(unique=true, nullable=false,length=SCHEMA_NAME_MAX_LENGTH,name="schema_name")
	@JsonProperty
	String schemaName;

	@Getter
	@Setter
	@NonNull
	@Column(nullable=false,length=16,name="username")
	String username;

	@Getter
	@Setter
	@NonNull
	@Column(nullable=false,length=36,name="password")
	String password = UUID.randomUUID().toString();

	@ManyToOne(optional = false, fetch=FetchType.EAGER)
	@Getter
	@Setter
	@NonNull
	//@JsonProperty
	Shard shard;
	
	@Getter
	@Setter
	@Column(nullable=false,name="active")
	@JsonProperty
	boolean	active = true;
	
	@OneToMany(mappedBy = "tenant",targetEntity=User.class)
	@Getter(AccessLevel.PROTECTED)
	private List<User> users;

	@Transient
	private EventListenerIntegrator eventListenerIntegrator = null;

	public Tenant() {
	}

	@Override
	public String getHibernateConfigurationFilename() {
		return ("hibernate/"+System.getProperty("tenant.hibernateconfig"));
	}
	
	protected void addUser(User u) {
		u.setTenant(this);
		users.add(u);
	}
	
	public void removeUser(User u) {
		u.setTenant(null);
		users.remove(u);
	}

	@Override
	public String getUrl() {
		return shard.getUrl();
	}

	@Override
	public String getChangeLogName() {
		return Tenant.TENANT_CHANGELOG_FILENAME;
	}
	
	public static boolean isValidSchemaName(String name) {
		if(name == null)
			return false;
		
		Matcher matcher = Tenant.SCHEMA_NAME_PATTERN.matcher(name);
		return matcher.matches();
	}
}
