package com.codeshelf.persistence;

import java.sql.Connection;

import com.mchange.v2.c3p0.AbstractConnectionCustomizer;

public class CsConnectionCustomizer extends AbstractConnectionCustomizer {

	@Override
	public void onAcquire(Connection c, String parentDataSourceIdentityToken) throws Exception {
		// per-connection mode setting here
	}

}
