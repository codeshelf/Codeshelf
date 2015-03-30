package com.codeshelf.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.integration.commandline.CommandLineUtils;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseUtils {
	static final private Logger	LOGGER						= LoggerFactory.getLogger(DatabaseUtils.class);

	public enum SQLSyntax {
		H2_MEMORY,POSTGRES,OTHER;
	}

	private DatabaseUtils() {}

	public static SQLSyntax getSQLSyntax(String url) {
		if(url.startsWith("jdbc:postgresql:")) {
			return DatabaseUtils.SQLSyntax.POSTGRES;
		} else if(url.startsWith("jdbc:h2:mem")) {
			return DatabaseUtils.SQLSyntax.H2_MEMORY;
		} else {
			return DatabaseUtils.SQLSyntax.OTHER;
		}
	}
	
	public static DatabaseUtils.SQLSyntax getSQLSyntax(DatabaseCredentials conn) {
		return getSQLSyntax(conn.getUrl());
	}

	public static Connection getConnection(DatabaseCredentials cred) throws SQLException {
		return DriverManager.getConnection(
			cred.getUrl(),
			cred.getUsername(),
			cred.getPassword());
	}

	public static Database getAppDatabase(DatabaseCredentials cred) {
	    Database appDatabase;
		try {
			appDatabase = CommandLineUtils.createDatabaseObject(ClassLoader.getSystemClassLoader(),
				cred.getUrl(), 
				cred.getUsername(), 
				cred.getPassword(), null, 
				null, cred.getSchemaName(),
				false, false,
				null,null,
				null,null);
		} catch (DatabaseException e1) {
			LOGGER.error("Database exception evaluating app database configuration", e1);
			return null;
		} 
		return appDatabase;
	}

	public static void executeSQL(DatabaseCredentials cred, String sql) throws SQLException {
		Connection conn = getConnection(cred);
		Statement stmt = conn.createStatement();
		LOGGER.trace("Executing explicit SQL: "+sql);
		stmt.execute(sql);
		stmt.close();
		conn.close();
	}

	public static void Hbm2DdlSchemaExport(Configuration configuration, DatabaseCredentials tenant) {
		Connection conn;
		try {
			conn = getConnection(tenant);
		} catch (SQLException e2) {
			LOGGER.error("Failed to get connection for schema export, cannot continue.",e2);
			throw new RuntimeException(e2);
		}
		SchemaExport se = new SchemaExport(configuration,conn);
		se.create(false, true);
	}
	
	
}
