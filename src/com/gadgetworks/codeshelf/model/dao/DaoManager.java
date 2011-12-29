/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DaoManager.java,v 1.1 2011/12/29 09:15:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.List;

public class DaoManager {

	public static DaoManager	gDaoManager	= new DaoManager();

	private List<IDAOListener>	mListeners	= new ArrayList<IDAOListener>();

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#registerDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void registerDAOListener(IDAOListener inListener) {
		mListeners.add(inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void unregisterDAOListener(IDAOListener inListener) {
		mListeners.remove(inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void removeDAOListeners() {
		mListeners.clear();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	public void objectAdded(final Object inObject) {
		for (IDAOListener daoListener : mListeners) {
			daoListener.objectAdded(inObject);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	public void objectUpdated(final Object inObject) {
		for (IDAOListener daoListener : mListeners) {
			daoListener.objectUpdated(inObject);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	public void objectDeleted(final Object inObject) {
		for (IDAOListener daoListener : mListeners) {
			daoListener.objectDeleted(inObject);
		}
	}

}
