/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DaoException.java,v 1.3 2012/03/17 09:07:02 jeffw Exp $
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
