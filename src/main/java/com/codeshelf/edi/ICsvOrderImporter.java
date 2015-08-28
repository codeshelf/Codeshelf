/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsvImporter.java,v 1.2 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.validation.BatchResult;

/**
 * @author jeffw
 *
 */
public interface ICsvOrderImporter {

	BatchResult<Object> importOrdersFromCsvStream(Reader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) throws IOException;

	// here for easier testablity
	int toInteger(final String inString);
	
	void setTruncatedGtins(boolean value);
	
	void persistDataReceipt(Facility facility, String username, String filename, long receivedTime,  BatchResult<?> result);

}
