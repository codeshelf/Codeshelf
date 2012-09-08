/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.6 2012/09/08 03:03:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
public class Tier extends LocationABC {

	@Inject
	public static ITypedDao<Tier>	DAO;

	@Singleton
	public static class TierDao extends GenericDaoABC<Tier> implements ITypedDao<Tier> {
		public final Class<Tier> getDaoClass() {
			return Tier.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Tier.class);

	public Tier(final Double inPosX, final double inPosY) {
		super(PositionTypeEnum.METERS_FROM_PARENT, inPosX, inPosY);
	}

	@JsonIgnore
	public final ITypedDao<Tier> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "T";
	}

	public final IDomainObject getParent() {
		return getParentBay();
	}

	public final void setParent(IDomainObject inParent) {
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
