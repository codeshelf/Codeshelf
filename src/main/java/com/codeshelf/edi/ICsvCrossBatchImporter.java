/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsvImporter.java,v 1.2 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;

/**
 * @author jeffw
 *
 */
public interface ICsvCrossBatchImporter {

	/**
	 * @return the number of successfully imported records
	 */
	int importCrossBatchesFromCsvStream(Tenant tenant,Reader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime);

}
