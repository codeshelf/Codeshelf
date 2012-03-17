/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DaoManager.java,v 1.2 2012/03/17 09:07:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.ArrayList;
import java.util.List;

public class DaoManager {

	public static DaoManager	gDaoManager	= new DaoManager();

	private List<IDaoListener>	mListeners	= new ArrayList<IDaoListener>();

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#registerDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void registerDAOListener(IDaoListener inListener) {
		mListeners.add(inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.ISystemDAO#unRegisterDAOListener(com.gadgetworks.codeshelf.model.dao.IDAOListener)
	 */
	public final void unregisterDAOListener(IDaoListener inListener) {
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
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectAdded(inObject);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	public void objectUpdated(final Object inObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectUpdated(inObject);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inObject
	 */
	public void objectDeleted(final Object inObject) {
		for (IDaoListener daoListener : mListeners) {
			daoListener.objectDeleted(inObject);
		}
	}

}
