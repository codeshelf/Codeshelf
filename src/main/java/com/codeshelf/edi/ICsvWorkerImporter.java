/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsvImporter.java,v 1.2 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;

import com.codeshelf.model.domain.Facility;

/**
 * @author jon ranstrom
 *
 */
public interface ICsvWorkerImporter {

	/**
	 * Import workers from file.
	 * @param inCsvStreamReader
	 * @param inFacility
	 * @param inAppend - if false, all existing workers not mentioned in this import will be deactivated
	 * @param inProcessTime
	 */
	boolean importWorkersFromCsvStream(Reader inCsvStreamReader, Facility inFacility, boolean inAppend, Timestamp inProcessTime);

}
