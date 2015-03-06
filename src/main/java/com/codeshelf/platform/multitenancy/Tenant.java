package com.codeshelf.platform.multitenancy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
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
@EqualsAndHashCode(callSuper = false, of={"tenantId","created"})
public class Tenant extends Schema {
	private static final String TENANT_CHANGELOG_FILENAME= "liquibase/db.changelog-master.xml";
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Tenant.class);

	@Id
	@Column(nullable = false,name="id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Getter
	@Setter
	int tenantId;

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
	@Column(unique=true, nullable=false,length=16,name="schema_name")
	@JsonProperty
	String schemaName;

	@Getter
	@Setter
	@NonNull
	@Column(nullable=false,length=16,name="username")
	@JsonProperty
	String username;

	@Getter
	@Setter
	@NonNull
	@Column(nullable=false,length=36,name="password")
	@JsonProperty
	String password = UUID.randomUUID().toString();

	@ManyToOne(optional = false, fetch=FetchType.EAGER)
	@Getter
	@Setter
	@NonNull
	@JsonProperty
	Shard shard;
	
	@Getter
	@Setter
	@Column(nullable=false,name="active")
	@JsonProperty
	boolean	active = true;
	
	@OneToMany(mappedBy = "tenant",targetEntity=User.class /*, fetch = FetchType.EAGER */)
	@MapKey(name = "username")
	private Map<String, User> users = new HashMap<String, User>();

	@Transient
	private EventListenerIntegrator eventListenerIntegrator = null;

	public Tenant() {
	}

	@Override
	public String getHibernateConfigurationFilename() {
		return ("hibernate/"+System.getProperty("tenant.hibernateconfig"));
	}

	@Override
	public String toString() {
		return "tenant #"+this.getTenantId()+" ("+this.getName()+") on shard "+this.getShard().getName()+"/"+this.getSchemaName();
	}
	
	
	protected void addUser(User u) {
		u.setTenant(this);
		users.put(u.getUsername(), u);
	}
	
	public void removeUser(User u) {
		u.setTenant(null);
		users.remove(u.getUsername());
	}
	
	public Collection<User> getUsers() {
		return new ArrayList<User>(users.values());
	}

	@Override
	public String getUrl() {
		return shard.getUrl();
	}

	@Override
	public String getChangeLogName() {
		return Tenant.TENANT_CHANGELOG_FILENAME;
	}
}
