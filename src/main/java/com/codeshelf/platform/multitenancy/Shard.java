package com.codeshelf.platform.multitenancy;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.platform.persistence.DatabaseConnection;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "shard")
@EqualsAndHashCode(callSuper = false,of={"shardId","created"})
public class Shard extends DatabaseConnection {
	private static final Logger LOGGER = LoggerFactory.getLogger(Shard.class);

	@Id
	@Column(nullable = false,name="id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Getter
	@Setter
	int shardId;

	/* Timestamped entity */
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="created")
	@JsonProperty
	Date created;
	//
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="last_modified")
	@JsonProperty
	Date lastModified;
	//
	@PrePersist
	protected void onCreate() { this.created = this.lastModified = new Date(); }
	@PreUpdate
	protected void onUpdate() { this.lastModified = new Date(); }
	/* Timestamped entity */
	
	@Column(nullable = false,length=255,name="name")
	@Getter 
	@Setter
	@NonNull
	@JsonProperty
	String name;
	
	@Column(nullable = false,name="url")
	@Getter 
	@Setter
	@NonNull
	@JsonProperty
	String url;
	
	@Column(nullable = false,length=16,name="username")
	@Getter
	@Setter
	String username;

	@Column(nullable = false,length=36,name="password")
	@Getter
	@Setter
	String password;

    @OneToMany(mappedBy = "shard", targetEntity=Tenant.class)
    @MapKey(name = "name")
	private Map<String, Tenant> tenants = new HashMap<String, Tenant>();

	public Shard() {
	}

	@Override
	public String toString() {
		return "shard "+this.shardId+" "+this.name;
	}

	public Tenant getTenant(String name) {
		return tenants.get(name);
	}

	private boolean createSchemaAndUser(Tenant newTenant) {
		boolean result = false;
		try {
			if(newTenant.getSQLSyntax() == DatabaseConnection.SQLSyntax.H2) {
				// use H2 syntax
				executeSQL("CREATE SCHEMA "+newTenant.getSchemaName()); 
				executeSQL("CREATE USER "+newTenant.getUsername()+" PASSWORD '"+newTenant.getPassword()+"'");
				executeSQL("ALTER USER "+newTenant.getUsername()+" ADMIN TRUE");
			} else {
				// assuming postgres syntax
				executeSQL("CREATE SCHEMA IF NOT EXISTS "+newTenant.getSchemaName());
				try {
					executeSQL("CREATE USER "+newTenant.getUsername()+" PASSWORD '"+newTenant.getPassword()+"'");
				} catch (SQLException e) {
					if(e.getMessage().equals("ERROR: role \""+newTenant.getUsername()+"\" already exists")) {
						LOGGER.warn("Tried to create user but already existed (assuming same password): "+newTenant.getUsername());
					} else {
						throw e;
					}
				}
				executeSQL("GRANT ALL ON SCHEMA "+newTenant.getSchemaName()+" TO "+newTenant.getUsername());
			}
			result = true;
		} catch (SQLException e) {
			LOGGER.error("SQL error creating tenant schema and user", e);
		}
		return result;
	}

	public Tenant createTenant(String name,String schemaName,String username,String password) {
		// must be called within active transaction

		Tenant result = null;	
		
		if(canCreateTenant(name,schemaName)) {
			// tenant does not already exist with this 
			Tenant tenant = new Tenant();
			tenant.setName(name);
			tenant.setUsername(username);
			tenant.setSchemaName(schemaName);
			tenant.setPassword(password);
			tenant.setShard(this);

			if(createSchemaAndUser(tenant)) { // automatically generated password
				tenants.put(name, tenant);
				ManagerPersistenceService.getInstance().getSession().save(tenant);
				result = tenant;
			} else {
				LOGGER.error("Failed to create schema/username "+username);
			}
		} else {
			LOGGER.error("Tried to create tenant that already exists: "+name);
		}
		return result;
	}

	private boolean canCreateTenant(String name, String schemaName) {
		for(Tenant tenant : this.tenants.values()) {
			if(name.equals(tenant.getName())
					|| schemaName.equals(tenant.getSchemaName()) ) {
				return false;
			}
		}
		return true;
	}

}
