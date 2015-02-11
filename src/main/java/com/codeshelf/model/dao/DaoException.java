/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DaoException.java,v 1.5 2012/10/03 06:39:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class DaoException extends RuntimeException {

	private static final long	serialVersionUID	= -8652996336581739434L;

	public DaoException(String msg) {
		super(msg);
	}
}
