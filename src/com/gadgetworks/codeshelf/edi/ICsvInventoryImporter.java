/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsvImporter.java,v 1.2 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.InputStreamReader;
import java.sql.Timestamp;

import com.gadgetworks.codeshelf.model.domain.Facility;

/**
 * @author jeffw
 *
 */
public interface ICsvInventoryImporter {

	void importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime);

	void importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime);

}
