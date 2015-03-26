package com.codeshelf.platform.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatabaseConnection {
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);

	public enum SQLSyntax {
		H2,POSTGRES,OTHER;
	}
	
	protected abstract String getUrl();
	protected abstract String getUsername();
	protected abstract String getPassword();

	public SQLSyntax getSQLSyntax() {
		String url = this.getUrl();
		if(url.startsWith("jdbc:postgresql:")) {
			return DatabaseConnection.SQLSyntax.POSTGRES;
		} else if(url.startsWith("jdbc:h2:mem")) {
			return DatabaseConnection.SQLSyntax.H2;
		} else {
			return DatabaseConnection.SQLSyntax.OTHER;
		}
	}

	public void executeSQL(String sql) throws SQLException {
		Connection conn = DriverManager.getConnection(
			this.getUrl(),
			this.getUsername(),
			this.getPassword());
		Statement stmt = conn.createStatement();
		LOGGER.trace("Executing explicit SQL: "+sql);
		stmt.execute(sql);
		stmt.close();
		conn.close();
	}
}
