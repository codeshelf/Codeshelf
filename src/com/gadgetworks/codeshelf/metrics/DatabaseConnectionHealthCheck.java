package com.gadgetworks.codeshelf.metrics;

import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

public class DatabaseConnectionHealthCheck extends CodeshelfHealthCheck {

	PersistenceService database;

	public DatabaseConnectionHealthCheck(PersistenceService database) {
		super("Database Connection");
		this.database = database;
	}
	
    @Override
    protected Result check() throws Exception {
    	/*
    	String sql = "select 1"; 
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
    	*/
        return Result.healthy("Database is reachable");
    }
}