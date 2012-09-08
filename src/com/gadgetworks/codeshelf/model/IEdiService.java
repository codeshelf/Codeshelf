/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiService.java,v 1.1 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;

public interface IEdiService extends IDomainObject {

	EdiServiceStateEnum getServiceStateEnum();
	
	void updateDocuments();
	
}
