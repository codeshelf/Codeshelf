/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.2 2012/07/22 08:49:37 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
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

	@Inject
	public static ITypedDao<Bay> DAO;
	
	public final ITypedDao<Bay> getDao() {
		return DAO;
	}

	public Bay(final Double inPosX, final double inPosY, final double inPosZ) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY, inPosZ);
	}
	
	public String getDefaultDomainIdPrefix() {
		return "B";
	}

	public final IDomainObject getParent() {
		return getParentLocation();
	}

	public final void setParent(IDomainObject inParent) {
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
