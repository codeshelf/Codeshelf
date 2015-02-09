package com.gadgetworks.codeshelf.platform.persistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaManager {
	private static final Logger LOGGER	= LoggerFactory.getLogger(SchemaManager.class);

	private String changeLogName;
	private String url;
	private String username;
	private String password;
	private String schemaName;
	private String hibernateConfigurationFile;
	
	public SchemaManager(String changeLogName, String url,String username,String password,String schemaName,String hibernateConfigurationFile) {
		this.changeLogName = changeLogName;
		this.url = url;
		this.username = username;
		this.password = password;
		this.schemaName = schemaName;
		this.hibernateConfigurationFile = hibernateConfigurationFile;
	}
	
	public void applySchemaUpdates() {
		Database appDatabase = getAppDatabase();
		if(appDatabase==null) {
			throw new RuntimeException("Failed to access app database, cannot continue");
		}
		
		ResourceAccessor fileOpener = new ClassLoaderResourceAccessor(); 
		
		LOGGER.info("initializing Liquibase");
		Contexts contexts = new Contexts(); //empty context
		Liquibase liquibase;
		try {
			liquibase = new Liquibase(changeLogName, fileOpener, appDatabase);
		} catch (LiquibaseException e) {
			LOGGER.error("Failed to initialize liquibase, cannot continue.", e);
			throw new RuntimeException("Failed to initialize liquibase, cannot continue.",e);
		}

		List<ChangeSet> pendingChanges;
		try {
			pendingChanges = liquibase.listUnrunChangeSets(contexts);
		} catch (LiquibaseException e1) {
			LOGGER.error("Could not get pending schema changes, cannot continue.", e1);
			throw new RuntimeException("Could not get pending schema changes, cannot continue.",e1);
		}
		
		if(pendingChanges.size() > 0) {	
			LOGGER.info("Now updating db schema - will apply "+pendingChanges.size()+" changesets");
			try {
				liquibase.update(contexts);
			} catch (LiquibaseException e) {
				LOGGER.error("Failed to apply changes to app database, cannot continue. Database might be corrupt.", e);
				throw new RuntimeException("Failed to apply changes to app database, cannot continue. Database might be corrupt.",e);
			}
			LOGGER.info("Done applying Liquibase changesets");
		} else {
			LOGGER.info("No pending Liquibase changesets");
		}
	}
	
	public boolean checkSchema() {
		
		// TODO: this, but cleanly without using unsupported CommandLineUtils interface
		Database hibernateDatabase;
		try {
			hibernateDatabase = CommandLineUtils.createDatabaseObject(ClassLoader.getSystemClassLoader(),
				"hibernate:classic:"+this.hibernateConfigurationFile, 
				null, null, null, 
				null, null,
				false, false,
				null,null,
				null,null);
		} catch (DatabaseException e1) {
			LOGGER.error("Database exception evaluating Hibernate configuration", e1);
			return false;
		}
		
        /*
        CommandLineUtils.createDatabaseObject(classLoader, url, 
        	username, password, driver, 
        	defaultCatalogName, defaultSchemaName, 
        	Boolean.parseBoolean(outputDefaultCatalog), Boolean.parseBoolean(outputDefaultSchema), 
        	null, null, 
        	this.liquibaseCatalogName, this.liquibaseSchemaName);
         */

		Database appDatabase = getAppDatabase();
		if(appDatabase==null) {
			return false;
		}
        
		DiffGeneratorFactory diffGen = DiffGeneratorFactory.getInstance();

		DiffResult diff;
		try {
			diff = diffGen.compare(hibernateDatabase, appDatabase, CompareControl.STANDARD);
		} catch (LiquibaseException e1) {
			LOGGER.error("Liquibase exception diffing Hibernate/database configuration", e1);
			return false;
		}
		
		DiffOutputControl diffOutputCtrl =  new DiffOutputControl();
		diffOutputCtrl.setIncludeCatalog(false);
		diffOutputCtrl.setIncludeSchema(false);
		
		DiffToChangeLog diff2cl = new DiffToChangeLog(diff,diffOutputCtrl);

		if(diff2cl.generateChangeSets().size() > 0) {
			try {
				diff2cl.print(System.out);
			} catch (ParserConfigurationException | IOException | DatabaseException e) {
				LOGGER.error("Unexpected exception outputing diff", e);
			}
			return false;
		} //else
		return true;
	}
	
	Database getAppDatabase() {
        Database appDatabase;
		try {
			appDatabase = CommandLineUtils.createDatabaseObject(ClassLoader.getSystemClassLoader(),
				url, 
				username, password, null, 
				null, schemaName,
				false, false,
				null,null,
				null,null);
		} catch (DatabaseException e1) {
			LOGGER.error("Database exception evaluating app database configuration", e1);
			return null;
		} 
		return appDatabase;
	}        		

	public void executeSQL(String sql) throws SQLException {
		Connection conn = DriverManager.getConnection(url,username,password);
		Statement stmt = conn.createStatement();
		LOGGER.trace("Executing explicit SQL: "+sql);
		stmt.execute(sql);
		stmt.close();
		conn.close();
	}

	public void deleteOrdersWis() throws SQLException {
		LOGGER.warn("Deleting all orders and work instructions from schema "+schemaName);
		executeSQL("UPDATE "+schemaName+".order_header SET container_use_persistentid=null");
		executeSQL("DELETE FROM "+schemaName+".container_use");
		executeSQL("DELETE FROM "+schemaName+".work_instruction");
		executeSQL("DELETE FROM "+schemaName+".container");
		executeSQL("DELETE FROM "+schemaName+".order_location");
		executeSQL("DELETE FROM "+schemaName+".order_detail");
		executeSQL("DELETE FROM "+schemaName+".order_header");
		executeSQL("DELETE FROM "+schemaName+".order_group");
	}

	public void dropSchema() throws SQLException {
		LOGGER.warn("Deleting entire schema "+schemaName);
		executeSQL("DROP SCHEMA "+schemaName+
			(isH2Mem()?"":" CASCADE"));
	}

	private boolean isH2Mem() {
		return (this.url.startsWith("jdbc:h2:mem") );
	}

	public void createSchemaIfNotExists() throws SQLException {
		executeSQL("CREATE SCHEMA IF NOT EXISTS "+schemaName);
	}
}
