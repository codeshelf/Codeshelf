package com.codeshelf.platform.persistence;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.security.CodeshelfSecurityManager;

public class CsCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

	@Override
	public String resolveCurrentTenantIdentifier() {
		User user = CodeshelfSecurityManager.getCurrentUser();
		Tenant tenant;
		if(user != null) {
			tenant = user.getTenant();
		} else {
			tenant = TenantManagerService.getInstance().getDefaultTenant();
		}
		return tenant.getSchemaName();
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return false;
	}

}
