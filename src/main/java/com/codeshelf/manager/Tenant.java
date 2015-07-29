package com.codeshelf.manager;

import java.util.Date;
import java.util.List;
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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.JvmProperties;
import com.codeshelf.persistence.DatabaseCredentials;
import com.codeshelf.persistence.DatabaseUtils;
import com.codeshelf.persistence.DatabaseUtils.SQLSyntax;
import com.codeshelf.persistence.EventListenerIntegrator;
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
public class Tenant implements DatabaseCredentials {

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
	@NaturalId
	String schemaName;

	@Getter
	@NonNull
	@Column(nullable=false,length=16,name="username")
	String username;

	@Getter
	@NonNull
	@Column(nullable=false,length=36,name="password")
	String password;

	@ManyToOne(optional = false, fetch=FetchType.EAGER)
	@Getter
	@NonNull
	//@JsonProperty
	Shard shard;
	
	@Getter
	@Setter
	@Column(nullable=false,name="active")
	@JsonProperty
	boolean	active = true;
	
	@OneToMany(mappedBy = "tenant",targetEntity=User.class)
	@Getter
	private List<User> users;

	@Transient
	private EventListenerIntegrator eventListenerIntegrator = null;

	public Tenant() {
		super();
	}
	
	public Tenant(String name, String username, String schemaName, String password, Shard shard) {
		super();
		this.name = name;
		this.schemaName = schemaName;
		this.username = username;
		this.password = password;
		this.shard = shard;
	}
	
	public void addUser(User u) {
		u.setTenant(this);
		users.add(u);
	}
	
	public void removeUser(User u) {
		u.setTenant(null);
		users.remove(u);
	}

	@Override
	public String getUrl() {
		String url = shard.getUrl();

		// schema setting for H2
		if(DatabaseUtils.getSQLSyntax(url).equals(SQLSyntax.H2_MEMORY)) {
			url += ";schema="+this.getSchemaName();
		}

		return url;
	}
	
	public static boolean isValidSchemaName(String name) {
		if(name == null)
			return false;
		
		Matcher matcher = Tenant.SCHEMA_NAME_PATTERN.matcher(name);
		return matcher.matches();
	}
	
	public String getTenantIdentifier() {
		// defined as schema name which is required to be unique
		return this.getSchemaName();
	}

	public String getAppServerVersion() {
		// TODO: return correct app server version for this tenant
		// today, assuming that version = tenant manager version
		return JvmProperties.getVersionStringShort();
	}

	public boolean clientVersionIsCompatible(String version) {
		return this.getAppServerVersion().equals(version);
	}
}
