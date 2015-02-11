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
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.platform.persistence.EventListenerIntegrator;
import com.codeshelf.platform.persistence.Schema;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Entity
@Table(name="tenant")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@JsonIgnoreProperties({"className"})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
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

	@Getter
	@Column(nullable = false,name="created_on")
	Date createdOn = new Date();
	
	@Getter 
	@Setter
	@NonNull
	@Column(unique=true, nullable=false,length=255,name="name")
	String name;

	@Getter
	@Setter
	@NonNull
	@Column(unique=true, nullable=false,length=16,name="db_schema_name")
	String schemaName;

	@Getter
	@Setter
	@NonNull
	@Column(unique=true, nullable=false,length=16,name="db_username")
	String username;

	@Getter
	@Setter
	@NonNull
	@Column(unique=true,nullable=false,length=36,name="db_password")
	String password = UUID.randomUUID().toString();

	@ManyToOne(optional = false, fetch=FetchType.EAGER)
	@Getter
	@Setter
	Shard shard;
	
	@OneToMany(mappedBy = "tenant",targetEntity=User.class)
	@MapKey(name = "username")
	private Map<String, User> users = new HashMap<String, User>();

	@Transient
	Configuration hibernateConfiguration = null;

	@Transient
	private EventListenerIntegrator eventListenerIntegrator = null;
	
	public Tenant() {
	}
	/*
	public SchemaManager getSchemaManager() {
		if(schemaManager == null) {
			schemaManager = new SchemaManager(TENANT_CHANGELOG_FILENAME, 
				shard.getDbUrl(), this.getUsername(), this.getPassword(), this.getSchemaName(),
				this.getHibernateConfigurationFilename());
		}
		return schemaManager;
	}
*/
	@Override
	public String getHibernateConfigurationFilename() {
		return ("hibernate/"+System.getProperty("tenant.hibernateconfig"));
	}

	@Override
	public String toString() {
		return "tenant #"+this.getTenantId()+" ("+this.getName()+") on shard "+this.getShard().getName()+"/"+this.getSchemaName();
	}
	
	public void addUser(User u) {
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((schemaName == null) ? 0 : schemaName.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((shard == null) ? 0 : shard.hashCode());
		result = prime * result + tenantId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tenant other = (Tenant) obj;
		if (createdOn == null) {
			if (other.getCreatedOn() != null)
				return false;
		} else if (!createdOn.equals(other.getCreatedOn()))
			return false;
		if (password == null) {
			if (other.getPassword() != null)
				return false;
		} else if (!password.equals(other.getPassword()))
			return false;
		if (schemaName == null) {
			if (other.getSchemaName() != null)
				return false;
		} else if (!schemaName.equals(other.getSchemaName()))
			return false;
		if (username == null) {
			if (other.getUsername() != null)
				return false;
		} else if (!username.equals(other.getUsername()))
			return false;
		if (name == null) {
			if (other.getName() != null)
				return false;
		} else if (!name.equals(other.getName()))
			return false;
		if (shard == null) {
			if (other.getShard() != null)
				return false;
		} else if (!shard.equals(other.getShard()))
			return false;
		if (tenantId != other.getTenantId())
			return false;
		return true;
	}

	@Override
	public String getUrl() {
		return shard.getDbUrl();
	}

	@Override
	public String getChangeLogName() {
		return Tenant.TENANT_CHANGELOG_FILENAME;
	}
}
