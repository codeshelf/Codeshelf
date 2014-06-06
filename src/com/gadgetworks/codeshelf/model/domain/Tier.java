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


//--------------------------------------------------------------------------
/**
* TierIds
* Just a means to allow tier method to return multi-value
* 
*/
final class TierIds {
	   String aisleName;
	   String bayName;
	   String tierName;
	}


//--------------------------------------------------------------------------
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
	
	private final TierIds getTierIds(){
		TierIds theTierIds = new TierIds();
		theTierIds.bayName = "";
		theTierIds.aisleName = "";
		theTierIds.tierName = this.getDomainId();
		Bay bayLocation = this.getParent();
		Aisle aisleLocation = null;
				
		if (bayLocation != null) {
			theTierIds.bayName = bayLocation.getDomainId();
			aisleLocation = bayLocation.getParent();
		}
		if (aisleLocation != null) {
			theTierIds.aisleName = aisleLocation.getDomainId();
		}

		return theTierIds;
	}

	public final String getBaySortName() {
		// to support list view meta-field baySortName
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.bayName + "-" + theTierIds.tierName);
	}
		
	public final String getTierSortName() {
		// to support list view meta-field tierSortName
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.tierName + "-" + theTierIds.bayName);
	}

}
