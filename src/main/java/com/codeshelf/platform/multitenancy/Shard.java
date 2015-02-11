package com.codeshelf.platform.multitenancy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.platform.persistence.ManagerPersistenceService;

@Entity
@Table(name = "shard")
public class Shard {
	private static final Logger LOGGER = LoggerFactory.getLogger(Shard.class);

	@Id
	@Column(nullable = false,name="id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Getter
	@Setter
	int shardId;

	@Column(nullable = false,length=255,name="name")
	@Getter 
	@Setter
	@NonNull
	String name;
	
	@Column(nullable = false,name="db_url")
	@Getter 
	@Setter
	@NonNull
	String dbUrl;
	
	@Column(nullable = false,length=16,name="db_admin_username")
	@Getter
	@Setter
	@NonNull
	String dbAdminUsername;

	@Column(nullable = false,length=36,name="db_admin_password")
	@Getter
	@Setter
	@NonNull
	String dbAdminPassword;

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

	private void executeSQL(String sql) throws SQLException {
		Connection conn = DriverManager.getConnection(this.getDbUrl(),this.getDbAdminUsername(),this.getDbAdminPassword());
		Statement stmt = conn.createStatement();
		LOGGER.trace("Executing explicit SQL: "+sql);
		stmt.execute(sql);
		stmt.close();
		conn.close();
	}

	private boolean createSchemaAndUser(String dbSchemaName, String dbUsername, String dbPassword) {
		boolean result = false;
		try {
			if(isH2MemShard()) {
				// use H2 syntax
				executeSQL("CREATE SCHEMA "+dbSchemaName); 
				executeSQL("CREATE USER "+dbUsername+" PASSWORD '"+dbPassword+"'");
				executeSQL("ALTER USER "+dbUsername+" ADMIN TRUE");
			} else {
				// assuming postgres syntax
				executeSQL("CREATE SCHEMA IF NOT EXISTS "+dbSchemaName);
				try {
					executeSQL("CREATE USER "+dbUsername+" PASSWORD '"+dbPassword+"'");
				} catch (SQLException e) {
					if(e.getMessage().equals("ERROR: role \""+dbUsername+"\" already exists")) {
						LOGGER.warn("Tried to create user but already existed (assuming same password): "+dbUsername);
					} else {
						throw e;
					}
				}
				executeSQL("GRANT ALL ON SCHEMA "+dbSchemaName+" TO "+dbUsername);
			}
			result = true;
		} catch (SQLException e) {
			LOGGER.error("SQL error creating tenant schema and user", e);
		}
		return result;
	}

	private boolean isH2MemShard() {
		return this.getDbUrl().startsWith("jdbc:h2:mem");
	}

	public Tenant createTenant(String name,String schemaName,String username,String password) {
		// must be called within active transaction

		Tenant result = null;	
		
		if(canCreateTenant(name,schemaName)) {
			// tenant does not already exist with this 
			Tenant tenant = new Tenant();
			tenant.setName(name);
			tenant.setDbUsername(username);
			tenant.setDbSchemaName(schemaName); 			
			if(password != null) {
				tenant.setDbPassword(password);
			}

			if(createSchemaAndUser(schemaName,username,tenant.getDbPassword())) { // automatically generated password
				tenant.setShard(this);
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
					|| schemaName.equals(tenant.getDbSchemaName()) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((dbAdminPassword == null) ? 0 : dbAdminPassword.hashCode());
		result = prime * result + ((dbAdminUsername == null) ? 0 : dbAdminUsername.hashCode());
		result = prime * result + ((dbUrl == null) ? 0 : dbUrl.hashCode());
		result = prime * result + shardId;
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
		Shard other = (Shard) obj;
		if (name == null) {
			if (other.getName() != null)
				return false;
		} else if (!name.equals(other.getName()))
			return false;
		if (dbAdminPassword == null) {
			if (other.getDbAdminPassword() != null)
				return false;
		} else if (!dbAdminPassword.equals(other.getDbAdminPassword()))
			return false;
		if (dbAdminUsername == null) {
			if (other.getDbAdminUsername() != null)
				return false;
		} else if (!dbAdminUsername.equals(other.getDbAdminUsername()))
			return false;
		if (dbUrl == null) {
			if (other.getDbUrl() != null)
				return false;
		} else if (!dbUrl.equals(other.getDbUrl()))
			return false;
		if (shardId != other.getShardId())
			return false;
		return true;
	}

}
