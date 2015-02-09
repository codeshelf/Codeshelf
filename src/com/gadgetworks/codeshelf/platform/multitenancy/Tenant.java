package com.gadgetworks.codeshelf.platform.multitenancy;

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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.platform.persistence.SchemaManager;

@Entity
@Table(name="tenant")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@JsonIgnoreProperties({"className"})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Tenant {
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
	String dbSchemaName;

	@Getter
	@Setter
	@NonNull
	@Column(unique=true, nullable=false,length=16,name="db_username")
	String dbUsername;

	@Getter
	@Setter
	@NonNull
	@Column(unique=true,nullable=false,length=36,name="db_password")
	String dbPassword = UUID.randomUUID().toString();

	@ManyToOne(optional = false, fetch=FetchType.EAGER)
	@Getter
	@Setter
	Shard shard;
	
	@OneToMany(mappedBy = "tenant",targetEntity=User.class)
	@MapKey(name = "username")
	private Map<String, User> users = new HashMap<String, User>();

	@Transient
	SchemaManager schemaManager = null;
	
	@Transient
	Configuration hibernateConfiguration;
	
	public Tenant() {
	}
	
	public SchemaManager getSchemaManager() {
		if(schemaManager == null) {
			schemaManager = new SchemaManager(PersistenceService.getInstance().getChangeLogFilename(), 
				shard.getDbUrl(), this.getDbUsername(), this.getDbPassword(), this.getDbSchemaName(),
				this.getHibernateConfigurationFile());
		}
		return schemaManager;
	}
	
	public Configuration getHibernateConfiguration() {
		if(this.hibernateConfiguration == null) {
			// fetch database config from properties file
			hibernateConfiguration = new Configuration().configure(getHibernateConfigurationFile());
	    	
			hibernateConfiguration .setProperty("hibernate.connection.url", getShard().getDbUrl());
			hibernateConfiguration .setProperty("hibernate.connection.username", getDbUsername());
			hibernateConfiguration .setProperty("hibernate.connection.password", getDbPassword());

	    	if(getDbSchemaName() != null) {
	    		hibernateConfiguration .setProperty("hibernate.default_schema", getDbSchemaName());
	    	}
	    	hibernateConfiguration .setProperty("javax.persistence.schema-generation-source","metadata-then-script");
		}
		return this.hibernateConfiguration;
	}

	public String getHibernateConfigurationFile() {
		return ("hibernate/"+System.getProperty("tenant.hibernateconfig"));
	}

	@Override
	public String toString() {
		return "tenant #"+this.getTenantId()+" ("+this.getName()+") on shard "+this.getShard().getName()+"/"+this.getDbSchemaName();
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
		result = prime * result + ((dbPassword == null) ? 0 : dbPassword.hashCode());
		result = prime * result + ((dbSchemaName == null) ? 0 : dbSchemaName.hashCode());
		result = prime * result + ((dbUsername == null) ? 0 : dbUsername.hashCode());
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
		if (dbPassword == null) {
			if (other.getDbPassword() != null)
				return false;
		} else if (!dbPassword.equals(other.getDbPassword()))
			return false;
		if (dbSchemaName == null) {
			if (other.getDbSchemaName() != null)
				return false;
		} else if (!dbSchemaName.equals(other.getDbSchemaName()))
			return false;
		if (dbUsername == null) {
			if (other.getDbUsername() != null)
				return false;
		} else if (!dbUsername.equals(other.getDbUsername()))
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
	
	
}
