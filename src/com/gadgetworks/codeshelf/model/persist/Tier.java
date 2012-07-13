/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.3 2012/07/13 08:08:41 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Tier
 * 
 * The object that models a tier within a bay.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "LOCATION")
@DiscriminatorValue("TIER")
public class Tier extends Location {

	private static final Log	LOGGER	= LogFactory.getLog(Tier.class);

	@Singleton
	public static class TierDao extends GenericDao<Tier> implements ITypedDao<Tier> {
		public TierDao() {
			super(Tier.class);
		}
	}

	@Inject
	public Tier(final TierDao inOrm) {
		super(inOrm);
	}
	
	public final PersistABC getParent() {
		return getParentBay();
	}
	
	public final void setParent(PersistABC inParent) {
		if (inParent instanceof Bay) {
			setParentLocation((Bay) inParent);
		}
	}
	
	public final Bay getParentBay() {
		return (Bay) getParentLocation();
	}
	
	public final void setParentBay(Bay inParentBay) {
		setParentLocation(inParentBay);
	}
}
