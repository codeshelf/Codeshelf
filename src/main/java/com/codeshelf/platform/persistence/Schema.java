package com.codeshelf.platform.persistence;

import java.io.IOException;
import java.sql.SQLException;
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

import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Schema extends DatabaseConnection {
	static final Logger LOGGER	= LoggerFactory.getLogger(Schema.class);

	public abstract String getSchemaName();
	public abstract String getHibernateConfigurationFilename();
	public abstract String getChangeLogName();

	public Configuration getHibernateConfiguration() {
		// fetch database config from properties file
		Configuration hibernateConfiguration = new Configuration().configure(this.getHibernateConfigurationFilename());
		
		hibernateConfiguration .setProperty("hibernate.connection.url", this.getUrl());
		hibernateConfiguration .setProperty("hibernate.connection.username", this.getUsername());
		hibernateConfiguration .setProperty("hibernate.connection.password", this.getPassword());
		hibernateConfiguration .setProperty("hibernate.default_schema", this.getSchemaName());
	
		// wait why this again
		hibernateConfiguration .setProperty("javax.persistence.schema-generation-source","metadata-then-script");
		return hibernateConfiguration;
	}
	
	void applyLiquibaseSchemaUpdates() {
		if(getSQLSyntax() != DatabaseConnection.SQLSyntax.POSTGRES) {
			LOGGER.debug("Will not attempt to apply Liquibase updates to non-Postgres schema");
			return;
		}
	
		try {
			this.executeSQL("CREATE SCHEMA IF NOT EXISTS "+this.getSchemaName());
		} catch (SQLException e) {
			throw new RuntimeException("Cannot start, failed to verify/create schema (check db admin rights)");
		}
	
		Database appDatabase = this.getAppDatabase();
		if(appDatabase==null) {
			throw new RuntimeException("Failed to access app database, cannot continue");
		}
		
		ResourceAccessor fileOpener = new ClassLoaderResourceAccessor(); 
		
		LOGGER.debug("initializing Liquibase");
		Contexts contexts = new Contexts(); //empty context
		Liquibase liquibase;
		try {
			liquibase = new Liquibase(this.getChangeLogName(), fileOpener, appDatabase);
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
			LOGGER.info("Now updating db schema - will apply "+pendingChanges.size()+" changesets to {}",this.getSchemaName());
			try {
				liquibase.update(contexts);
			} catch (LiquibaseException e) {
				LOGGER.error("Failed to apply changes to app database, cannot continue. Database might be corrupt.", e);
				throw new RuntimeException("Failed to apply changes to app database, cannot continue. Database might be corrupt.",e);
			}
			LOGGER.info("Done applying Liquibase changesets: {}",this.getSchemaName());
		} else {
			LOGGER.info("Liquibase initializing with 0 changesets: {}",this.getSchemaName());
		}
	
		if(!this.liquibaseCheckSchema()) {
			throw new RuntimeException("Cannot start, schema does not match");
		}
	}

	private Database getAppDatabase() {
	    Database appDatabase;
		try {
			appDatabase = CommandLineUtils.createDatabaseObject(ClassLoader.getSystemClassLoader(),
				this.getUrl(), 
				this.getUsername(), 
				this.getPassword(), null, 
				null, this.getSchemaName(),
				false, false,
				null,null,
				null,null);
		} catch (DatabaseException e1) {
			LOGGER.error("Database exception evaluating app database configuration", e1);
			return null;
		} 
		return appDatabase;
	}

	private boolean liquibaseCheckSchema() {
		
		// TODO: this, but cleanly without using unsupported CommandLineUtils interface
		Database hibernateDatabase;
		try {
			hibernateDatabase = CommandLineUtils.createDatabaseObject(ClassLoader.getSystemClassLoader(),
				"hibernate:classic:"+this.getHibernateConfigurationFilename(), 
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
	
		Database appDatabase = this.getAppDatabase();
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

}
