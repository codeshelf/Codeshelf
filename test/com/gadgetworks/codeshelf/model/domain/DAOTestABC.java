/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import org.junit.Before;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;

/**
 * @author jeffw
 *
 */
public class DAOTestABC {

	private IUtil			mUtil;
	protected ISchemaManager	mSchemaManager;
	protected IDatabase		mDatabase;

	@Before
	public final void setup() throws ClassNotFoundException {
		mUtil = new IUtil() {

			public void setLoggingLevelsFromPrefs(Organization inOrganization, ITypedDao<PersistentProperty> inPersistentPropertyDao) {
			}

			public String getVersionString() {
				return "";
			}

			public String getApplicationLogDirPath() {
				return ".";
			}

			public String getApplicationDataDirPath() {
				return ".";
			}

			public void exitSystem() {
				System.exit(-1);
			}
		};

		Class.forName("org.h2.Driver");
		mSchemaManager = new H2SchemaManager(mUtil, "codeshelf", "codeshelf", "codeshelf", "codeshelf", "localhost", "", "false");
		mDatabase = new Database(mSchemaManager, mUtil);

		mDatabase.start();
	}

}
