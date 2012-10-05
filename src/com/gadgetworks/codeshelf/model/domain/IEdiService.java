/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IEdiService.java,v 1.1 2012/10/05 21:01:40 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;

public interface IEdiService extends IDomainObject {

	EdiServiceStateEnum getServiceStateEnum();
	
	void updateDocuments();
	
}
