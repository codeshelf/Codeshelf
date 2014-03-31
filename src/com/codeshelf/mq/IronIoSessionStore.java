/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: file.java,v 1.1 2010/09/28 05:41:28 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.mq;

import com.dropbox.core.DbxSessionStore;

/**
 * @author jeffw
 *
 */
public class IronIoSessionStore implements DbxSessionStore {

	private String	mSessionKey;

	public IronIoSessionStore(String inSessionKey) {
		mSessionKey = inSessionKey;
	}

	public String get() {
		// TODO Auto-generated method stub
		return null;
	}

	public void set(String inValue) {
		// TODO Auto-generated method stub

	}

	public void clear() {
		// TODO Auto-generated method stub

	}

}
