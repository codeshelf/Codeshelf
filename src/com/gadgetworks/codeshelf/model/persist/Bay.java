/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.3 2012/07/13 08:08:41 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Bay
 * 
 * The object that models a storage bay in an aisle (pallet bay, etc.)
 * 
 * @author jeffw
 */

@Entity
@Table(name = "BAY")
@DiscriminatorValue("BAY")
public class Bay extends Location {

	private static final Log	LOGGER	= LogFactory.getLog(Bay.class);

	@Singleton
	public static class BayDao extends GenericDao<Bay> implements ITypedDao<Bay> {
		public BayDao() {
			super(Bay.class);
		}
	}

	public Bay(final BayDao inOrm) {
		super(inOrm);
	}

	public final PersistABC getParent() {
		return getParentLocation();
	}

	public final void setParent(PersistABC inParent) {
		if (inParent instanceof Aisle) {
			setParentLocation((Aisle) inParent);
		}
	}

	public final Aisle getParentAisle() {
		return (Aisle) getParentLocation();
	}

	public final void setParentAisle(Aisle inParentAisle) {
		setParentLocation(inParentAisle);
	}
}
