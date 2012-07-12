/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.2 2012/07/12 08:18:06 jeffw Exp $
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
	public static ITypedDao<Tier> DAO;
//	public static ITypedDao<Tier> DAO = new GenericDao<Tier>(Tier.class);

	public Tier() {

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
