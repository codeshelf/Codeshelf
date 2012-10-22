/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiService.java,v 1.4 2012/10/22 07:38:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import com.gadgetworks.codeshelf.edi.ICsvImporter;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;

public interface IEdiService {

	EdiServiceStateEnum getServiceStateEnum();
	
	void checkForCsvUpdates(ICsvImporter inCsvImporter);
	
}
