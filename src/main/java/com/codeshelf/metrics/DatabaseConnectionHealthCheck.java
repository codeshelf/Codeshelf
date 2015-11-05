package com.codeshelf.metrics;

import java.sql.SQLException;

import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.persistence.DatabaseUtils;

public class DatabaseConnectionHealthCheck extends CodeshelfHealthCheck {

	public DatabaseConnectionHealthCheck() {
		super("Database Connection");
	}
	
    @Override
    protected Result check() throws Exception {
    	SQLException ex = null;
    	try {
			DatabaseUtils.executeSQL(TenantManagerService.getInstance().getInitialTenant(),"SELECT 1;");
		} catch (SQLException e) {
			ex = e;
		}

    	if(ex == null) {
            return Result.healthy("Database is reachable");
    	} // else
        return unhealthy(ex);
    }
}