/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.15 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
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
@DiscriminatorValue("TIER")
@CacheStrategy(useBeanCache = false)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Tier extends SubLocationABC<Bay> {

	@Inject
	public static ITypedDao<Tier>	DAO;

	@Singleton
	public static class TierDao extends GenericDaoABC<Tier> implements ITypedDao<Tier> {
		@Inject
		public TierDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<Tier> getDaoClass() {
			return Tier.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Tier.class);

	public Tier(final Point inAnchorPoint, final Point inPickFaceEndPoint) {
		super(inAnchorPoint, inPickFaceEndPoint);
	}

	public final ITypedDao<Tier> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "T";
	}
	
	public final String getTierSortName() {
		// to support list view meta-field tierSortName
		String bayName = "";
		String aisleName = "";
		String tierName = this.getDomainId();
		Bay bayLocation = this.getParent();
		Aisle aisleLocation = null;
				
		if (bayLocation != null) {
			bayName = bayLocation.getDomainId();
			aisleLocation = bayLocation.getParent();
		}
		if (aisleLocation != null) {
			aisleName = aisleLocation.getDomainId();
		}
		return (aisleName + "-" + tierName + "-" + bayName);
	}

}
