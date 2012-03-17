/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DaoRegistry.java,v 1.1 2012/03/17 23:49:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class DaoRegistry implements IDaoRegistry {
	
	private List<IDao> mDaoList;
	
	public DaoRegistry() {
		mDaoList = new ArrayList<IDao>();
	}

	@Override
	public final void addDao(IDao inDaoRegister) {
		mDaoList.add(inDaoRegister);
	}

	@Override
	public final List<IDao> getDaoList() {
		return mDaoList;
	}

}
