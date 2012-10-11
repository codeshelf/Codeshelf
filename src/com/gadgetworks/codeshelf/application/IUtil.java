/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IUtil.java,v 1.1 2012/10/11 09:04:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;

/**
 * @author jeffw
 *
 */
public interface IUtil {
	
	String getVersionString();
	
	String getApplicationInitDatabaseURL();
	
	String getApplicationDataDirPath();
	
	String getApplicationDatabaseURL();
	
	String getApplicationLogDirPath();
	
	void setLoggingLevelsFromPrefs(Organization inOrganization, ITypedDao<PersistentProperty> inPersistentPropertyDao);

	void exitSystem();

}
