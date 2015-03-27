package com.codeshelf.platform.persistence;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.codeshelf.manager.Tenant;
import com.codeshelf.security.CodeshelfSecurityManager;

public class CsCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

	@Override
	public String resolveCurrentTenantIdentifier() {
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant(); 
		if(tenant == null) {
			throw new RuntimeException("Tenant resolver: No current tenant");
		}
		return tenant.getTenantIdentifier();
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return false; // true causes failure when cleaning up unclosed sessions
	}

}
