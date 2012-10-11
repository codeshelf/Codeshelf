/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiService.java,v 1.3 2012/10/11 02:42:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import com.gadgetworks.codeshelf.edi.IOrderImporter;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;

public interface IEdiService {

	EdiServiceStateEnum getServiceStateEnum();
	
	void checkForOrderUpdates(IOrderImporter inOrderImporter);
	
}
