/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.9 2012/10/21 02:02:17 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.avaje.ebean.annotation.CacheStrategy;
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
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
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

	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(final IDomainObject inParent) {
		if (inParent instanceof Bay) {
			parent = (Bay) inParent;
		}
	}

	public final ITypedDao<Tier> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "T";
	}
}
