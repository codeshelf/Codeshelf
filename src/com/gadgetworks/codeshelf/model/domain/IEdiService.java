/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiService.java,v 1.5 2013/03/15 14:57:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import com.gadgetworks.codeshelf.edi.ICsvImporter;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;

public interface IEdiService {

	EdiServiceStateEnum getServiceStateEnum();

	void checkForCsvUpdates(ICsvImporter inCsvImporter);

}
