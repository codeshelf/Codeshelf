/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DaoException.java,v 1.4 2012/09/08 03:03:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class DaoException extends Exception {

	private static final long	serialVersionUID	= -8652996336581739434L;

	public DaoException(String msg) {
		super(msg);
	}
}
