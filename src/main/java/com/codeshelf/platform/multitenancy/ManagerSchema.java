package com.codeshelf.platform.multitenancy;

import com.codeshelf.platform.persistence.IManagedSchema;

import lombok.Getter;

public class ManagerSchema implements IManagedSchema {
	private static final String MASTER_CHANGELOG_NAME = "liquibase/mgr.changelog-master.xml";

	@Getter
	private String	hibernateConfigurationFilename;
	@Getter
	private String	url;
	@Getter
	private String	username;
	@Getter
	private String	password;
	@Getter
	private String	schemaName;

	public ManagerSchema() {
		// fetch hibernate configuration from properties file
		this.hibernateConfigurationFilename = "hibernate/"+System.getProperty("manager.hibernateconfig"); // look in hibernate folder		
		this.url = System.getProperty("manager.db.url");
		this.username = System.getProperty("manager.db.username");
		this.password = System.getProperty("manager.db.password");		
		this.schemaName = System.getProperty("manager.db.schema");
	}
	
	@Override
	public String getChangeLogName() {
		return MASTER_CHANGELOG_NAME;
	}
}
