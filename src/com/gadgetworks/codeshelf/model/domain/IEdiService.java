/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiService.java,v 1.6 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;

public interface IEdiService {

	String getServiceName();

	EdiServiceStateEnum getServiceStateEnum();

	Boolean checkForCsvUpdates(ICsvOrderImporter inCsvOrdersImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationImporter inCsvLocationsImporter);

}
