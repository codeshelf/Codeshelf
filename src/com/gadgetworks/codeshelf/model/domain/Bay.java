/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Bay.java,v 1.5 2012/09/08 03:03:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@Table(name = "LOCATION")
@DiscriminatorValue("BAY")
public class Bay extends LocationABC {

	@Inject
	public static ITypedDao<Bay>	DAO;

	@Singleton
	public static class BayDao extends GenericDaoABC<Bay> implements ITypedDao<Bay> {
		public final Class<Bay> getDaoClass() {
			return Bay.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Bay.class);

	public Bay(final Aisle inAisle, final Double inPosX, final double inPosY, final double inPosZ) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY, inPosZ);
		setParentAisle(inAisle);
		setDomainId(getDefaultDomainId());
	}

	public final ITypedDao<Bay> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
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
