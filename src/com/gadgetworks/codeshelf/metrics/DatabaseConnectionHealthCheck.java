package com.gadgetworks.codeshelf.metrics;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;

public class DatabaseConnectionHealthCheck extends CodeshelfHealthCheck {

	IDatabase database;

	public DatabaseConnectionHealthCheck(IDatabase database) {
		super("Database Connection");
		this.database = database;
	}
	
    @Override
    protected Result check() throws Exception {
    	String sql = "select 1"; 
    	ISchemaManager schemaMgr = this.database.getSchemaManager();
    	Connection connection = schemaMgr.getConnection(schemaMgr.getApplicationInitDatabaseURL());
    	Statement statement = connection.createStatement();
    	boolean s = statement.execute(sql);
    	if (!s) {
            return Result.unhealthy("Failed to execute test query");
    	}
    	ResultSet result = statement.getResultSet();
    	if (!result.next()) {
            return Result.unhealthy("Empty result set");
    	}
    	int v = result.getInt(1);
    	if (v!=1) {
            return Result.unhealthy("Wrong value");
    	}
        return Result.healthy("Database is reachable");
    }
}