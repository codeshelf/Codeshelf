package com.codeshelf.metrics;

import java.sql.SQLException;

import com.codeshelf.platform.multitenancy.TenantManagerService;

public class DatabaseConnectionHealthCheck extends CodeshelfHealthCheck {

	public DatabaseConnectionHealthCheck() {
		super("Database Connection");
	}
	
    @Override
    protected Result check() throws Exception {
    	SQLException ex = null;
    	try {
			TenantManagerService.getInstance().getDefaultTenant().executeSQL("SELECT 1;");
		} catch (SQLException e) {
			ex = e;
		}

    	if(ex == null) {
            return Result.healthy("Database is reachable");
    	} // else
        return Result.unhealthy(ex);
    }
}