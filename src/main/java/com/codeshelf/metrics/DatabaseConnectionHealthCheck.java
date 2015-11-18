package com.codeshelf.metrics;

import java.sql.SQLException;

import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.DatabaseUtils;

public class DatabaseConnectionHealthCheck extends HealthCheckRefreshJob{
    @Override
    public void check(Facility facility) throws Exception {    	
    	SQLException ex = null;
    	try {
			DatabaseUtils.executeSQL(TenantManagerService.getInstance().getInitialTenant(),"SELECT 1;");
		} catch (SQLException e) {
			ex = e;
		}

    	if(ex == null) {
    		saveResults(facility, true, "Database is reachable");
    	} else {
    		saveResults(facility, false, ex.getMessage());
    	}
    }
}