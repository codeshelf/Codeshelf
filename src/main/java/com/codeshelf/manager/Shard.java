package com.codeshelf.manager;

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
import lombok.ToString;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.persistence.DatabaseCredentials;
import com.codeshelf.persistence.DatabaseUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "shard")
@EqualsAndHashCode(callSuper = false, of = { "name", "url", "username" })
@ToString(of = { "id", "name" }, callSuper = false)
public class Shard implements DatabaseCredentials {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Shard.class);

	@Id
	@Column(nullable = false, name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Getter
	@Setter
	@JsonProperty
	int							id;

	/* Timestamped entity */
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false, name = "created")
	@JsonProperty
	Date						created;
	//
	@Getter
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false, name = "last_modified")
	@JsonProperty
	Date						lastModified;

	//
	@PrePersist
	protected void onCreate() {
		this.created = this.lastModified = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
		this.lastModified = new Date();
	}

	/* Timestamped entity */

	@Column(nullable = false, length = 255, name = "name")
	@Getter
	@Setter
	@NonNull
	@JsonProperty
	String						name;

	@Column(nullable = false, name = "url")
	@Getter
	@Setter
	@NonNull
	String						url;

	@Column(nullable = false, length = 16, name = "username")
	@Getter
	@Setter
	String						username;

	@Column(nullable = false, length = 36, name = "password")
	@Getter
	@Setter
	String						password;

	@OneToMany(mappedBy = "shard", targetEntity = Tenant.class)
	@MapKey(name = "name")
	private Map<String, Tenant>	tenants	= new HashMap<String, Tenant>();

	public Shard() {
	}

	protected void addTenant(String name, Tenant tenant) {
		// should only be called be createTenant
		this.tenants.put(name,tenant);
	}

	public void removeTenant(Tenant tenant) {
		this.tenants.remove(tenant);
	}

	private boolean createSchemaAndUser(Tenant newTenant) {
		boolean result = false;
		try {
			if (DatabaseUtils.getSQLSyntax(this) == DatabaseUtils.SQLSyntax.H2_MEMORY) {
				// use H2 syntax
				DatabaseUtils.executeSQL(this,"CREATE SCHEMA " + newTenant.getSchemaName());
				DatabaseUtils.executeSQL(this,"CREATE USER " + newTenant.getUsername() + " PASSWORD '" + newTenant.getPassword() + "'");
				DatabaseUtils.executeSQL(this,"ALTER USER " + newTenant.getUsername() + " ADMIN TRUE");
			} else if (DatabaseUtils.getSQLSyntax(this) == DatabaseUtils.SQLSyntax.POSTGRES) {
				DatabaseUtils.executeSQL(this,"CREATE SCHEMA IF NOT EXISTS " + newTenant.getSchemaName());
				try {
					DatabaseUtils.executeSQL(this,"CREATE USER " + newTenant.getUsername() + " PASSWORD '" + newTenant.getPassword() + "'");
				} catch (SQLException e) {
					if (e.getMessage().equals("ERROR: role \"" + newTenant.getUsername() + "\" already exists")) {
						LOGGER.warn("Tried to create user {} for tenant {} but already existed (assuming same password)",
							newTenant.getUsername(),newTenant.getName());
					} else {
						throw e;
					}
				}
				DatabaseUtils.executeSQL(this,"GRANT ALL ON SCHEMA " + newTenant.getSchemaName() + " TO " + newTenant.getUsername());
			} else {
				throw new UnsupportedOperationException("unsupported database type for new tenant "+newTenant.getId()+" "+newTenant.getUrl());
			}
			result = true;
		} catch (SQLException e) {
			LOGGER.error("SQL error creating tenant schema and user", e);
		}
		return result;
	}

	protected Tenant createTenant(String name, String schemaName, String username, String password) {
		Tenant result = null;

		// this is called to initialize default tenant while TenantManagerService is starting
		// so, we call getMaybeRunningInstance which does not block
		if (TenantManagerService.getMaybeRunningInstance().canCreateTenant(name, schemaName)) {
			try {
				Session session = ManagerPersistenceService.getInstance().getSessionWithTransaction();

				Shard thisShard = (Shard) session.load(Shard.class,this.id);
				Tenant tenant = new Tenant(name,username,schemaName,password,thisShard);
				thisShard.addTenant(name, tenant);

				session.save(tenant);
				session.save(thisShard);

				if (createSchemaAndUser(tenant)) { // automatically generated password
					result = tenant;
				} else {
					LOGGER.error("Critical: Failed to create schema {} username {}",schemaName,username);
				}
			} finally {
				if (result == null) {
					ManagerPersistenceService.getInstance().rollbackTransaction();
				} else {
					ManagerPersistenceService.getInstance().commitTransaction();
				}
			}
		} else {
			LOGGER.error("Tried to create tenant that already exists: " + name);
		}
		return result;
	}

	@Override
	public String getSchemaName() {
		return "invalid";
	}

}
