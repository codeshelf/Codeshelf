package com.codeshelf.platform.persistence;

import java.util.HashMap;
import java.util.Properties;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;

public class CsMultiTenantConnectionProvider 
		extends AbstractMultiTenantConnectionProvider 
		implements ServiceRegistryAwareService {
	
	private static final long	serialVersionUID	= -1634590893951847320L;
	private HashMap<String,ConnectionProvider> connectionProviders;
	private ServiceRegistryImplementor	serviceRegistry = null;
	
	public CsMultiTenantConnectionProvider() {
		connectionProviders = new HashMap<String,ConnectionProvider>();
	}

    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        this.serviceRegistry  = serviceRegistry;
    }
    
    @Override
	protected ConnectionProvider getAnyConnectionProvider() {
    	String defaultSchemaName = TenantManagerService.getInstance().getDefaultTenant().getSchemaName();
		return selectConnectionProvider(defaultSchemaName);
	}

	@Override
	synchronized protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		ConnectionProvider cp = connectionProviders.get(tenantIdentifier);
		if(cp == null) {
			cp = createConnectionProvider(tenantIdentifier);
			connectionProviders.put(tenantIdentifier, cp);
		}
		return cp;
	}

	private ConnectionProvider createConnectionProvider(String tenantIdentifier) {
	    C3P0ConnectionProvider cp = new C3P0ConnectionProvider();
	    cp.injectServices(this.serviceRegistry);
	    Tenant tenant = TenantManagerService.getInstance().getTenantBySchema(tenantIdentifier);
	    Properties schemaProperties = tenant.getHibernateConfiguration().getProperties();
	    
	    cp.configure(schemaProperties );
	    return cp;
	}

}
