/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ILocation.java,v 1.2 2012/10/22 07:38:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;


/**
 * @author jeffw
 *
 */
public interface ILocation {
	
	void addItem(final String inItemId, Item inItem);
	
	Item getItem(final String inItemId);
	
	void removeItem(final String inItemId);
	
}