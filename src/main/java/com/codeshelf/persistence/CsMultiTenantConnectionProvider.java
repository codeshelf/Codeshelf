package com.codeshelf.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.persistence.DatabaseUtils.SQLSyntax;
import com.codeshelf.security.CodeshelfSecurityManager;

public class CsMultiTenantConnectionProvider 
		extends AbstractMultiTenantConnectionProvider 
		implements ServiceRegistryAwareService {
	
	static final Logger LOGGER	= LoggerFactory.getLogger(CsMultiTenantConnectionProvider.class);
	private static final long	serialVersionUID	= -1634590893951847320L;
	
	private ServiceRegistryImplementor	serviceRegistry = null; // provided by Hibernate framework
	
	SQLSyntax syntax;
	
	public CsMultiTenantConnectionProvider() {
		this.syntax = DatabaseUtils.getSQLSyntax(TenantManagerService.getInstance().getInitialTenant());
	}

    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        this.serviceRegistry  = serviceRegistry;
    }
    
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        final Connection connection = selectConnectionProvider(tenantIdentifier).getConnection();

        //nope
        //connection.setSchema(tenantIdentifier);
        
        if(syntax.equals(SQLSyntax.POSTGRES)) {
            connection.createStatement().execute("set search_path to '" + tenantIdentifier + "'");
        } else if(syntax.equals(SQLSyntax.H2_MEMORY)) {
        	connection.createStatement().execute("set schema " + tenantIdentifier);
        }

        return connection;
    }
    
    @Override
	protected ConnectionProvider getAnyConnectionProvider() {
    	Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
    	if(tenant == null) {
    		tenant = TenantManagerService.getInstance().getInitialTenant();
    	}
		return selectConnectionProvider(tenant.getTenantIdentifier());
	}

	@Override
	protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		// called during tenant persistence initialization
		// so just assume tenant persistence is running or at least started up enough to do this
		return TenantPersistenceService.getMaybeRunningInstance().getConnectionProvider(tenantIdentifier,serviceRegistry);
	}

}
