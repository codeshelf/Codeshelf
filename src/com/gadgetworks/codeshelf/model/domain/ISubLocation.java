/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ISubLocation.java,v 1.1 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

public interface ISubLocation<P extends IDomainObject> extends ILocation<P> {

	P getParent();

	void setParent(P inParent);

}
