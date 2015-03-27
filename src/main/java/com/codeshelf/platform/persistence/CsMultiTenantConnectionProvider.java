package com.codeshelf.platform.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.util.ConcurrentHashSet;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;

public class CsMultiTenantConnectionProvider 
		extends AbstractMultiTenantConnectionProvider 
		implements ServiceRegistryAwareService {
	
	static final Logger LOGGER	= LoggerFactory.getLogger(CsMultiTenantConnectionProvider.class);
	private static final long	serialVersionUID	= -1634590893951847320L;
	
	private ServiceRegistryImplementor	serviceRegistry = null; // provided by Hibernate framework

	private ConcurrentHashMap<String,ConnectionProvider> connectionProviders = new ConcurrentHashMap<String,ConnectionProvider>();
	private ConcurrentHashSet<String> initalizingConnectionProviders = new ConcurrentHashSet<String>();

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
		if(!connectionProviders.containsKey(tenantIdentifier)) {
			synchronized(this.initalizingConnectionProviders) {
				if(!this.initalizingConnectionProviders.contains(tenantIdentifier)) {
					this.initalizingConnectionProviders.add(tenantIdentifier);

					LOGGER.info("Creating connection to tenant {}",tenantIdentifier);
					Tenant tenant = TenantManagerService.getInstance().getTenantBySchemaName(tenantIdentifier);
					connectionProviders.put(tenantIdentifier, createConnectionProvider(tenant));
				} else {
					throw new RuntimeException("This shouldn't happen - not recursive and other threads should block");
				}
			}			
		}		
		return connectionProviders.get(tenantIdentifier);
	}

	private ConnectionProvider createConnectionProvider(Tenant tenant) {
	    // in synchronized, do not do long actions here

		// create connection provider for this tenant
		C3P0ConnectionProvider cp = new C3P0ConnectionProvider();
	    cp.injectServices(this.serviceRegistry);
	    
	    // configure the provider (we might be in init, trust that we can at least get a hibernate config)
	    Configuration genericConfiguration = TenantPersistenceService.getMaybeRunningInstance().getHibernateConfiguration();
	    Map<Object,Object> properties = new HashMap<Object,Object>(genericConfiguration.getProperties());
	    
	    properties.put("hibernate.connection.url", tenant.getUrl());
	    properties.put("hibernate.connection.username", tenant.getUsername());
	    properties.put("hibernate.connection.password", tenant.getPassword());
	    properties.put("hibernate.default_schema", tenant.getSchemaName());
	    
	    cp.configure(properties);
	    
	    return cp;
	}

}
