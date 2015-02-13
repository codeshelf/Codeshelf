package com.codeshelf.platform.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DatabaseConnection {

	public enum SQLSyntax {
		H2,POSTGRES,OTHER;
	}
	
	public abstract String getUrl();
	public abstract String getUsername();
	public abstract String getPassword();

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
		PersistenceService.LOGGER.trace("Executing explicit SQL: "+sql);
		stmt.execute(sql);
		stmt.close();
		conn.close();
	}


}
