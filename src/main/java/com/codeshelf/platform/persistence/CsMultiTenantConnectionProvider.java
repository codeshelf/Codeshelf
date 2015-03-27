package com.codeshelf.platform.persistence;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.TenantManagerService;

public class CsMultiTenantConnectionProvider 
		extends AbstractMultiTenantConnectionProvider 
		implements ServiceRegistryAwareService {
	
	static final Logger LOGGER	= LoggerFactory.getLogger(CsMultiTenantConnectionProvider.class);
	private static final long	serialVersionUID	= -1634590893951847320L;
	
	private ServiceRegistryImplementor	serviceRegistry = null; // provided by Hibernate framework

    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        this.serviceRegistry  = serviceRegistry;
    }
    
    @Override
	protected ConnectionProvider getAnyConnectionProvider() {
    	String defaultSchemaName = TenantManagerService.getInstance().getInitialTenant().getTenantIdentifier();
		return selectConnectionProvider(defaultSchemaName);
	}

	@Override
	protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		// called during tenant persistence initialization
		// so just assume tenant persistence is running or at least started up enough to do this
		return TenantPersistenceService.getMaybeRunningInstance().getConnectionProvider(tenantIdentifier,serviceRegistry);
	}

}
