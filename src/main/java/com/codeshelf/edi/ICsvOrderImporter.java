/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ICsvImporter.java,v 1.2 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.List;

import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.validation.BatchResult;

/**
 * @author jeffw
 *
 */
public interface ICsvOrderImporter {

	BatchResult<Object> importOrdersFromCsvStream(Reader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) throws IOException;
	
	BatchResult<Object> importOrdersFromCsvStream(Reader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime, boolean deleteOldOrders) throws IOException;
	
	BatchResult<Object> importOrdersFromBeanList(List<OutboundOrderCsvBean> originalBeanList, Facility facility, Timestamp inProcessTime, boolean deleteOldOrders);

	// here for easier testablity
	int toInteger(final String inString);
	
	void setTruncatedGtins(boolean value);
	
	void persistDataReceipt(Facility facility, String username, String filename, long receivedTime, EdiTransportType tranportType, BatchResult<?> result);
	
	void makeOrderDeletionFail(boolean fail);
}
