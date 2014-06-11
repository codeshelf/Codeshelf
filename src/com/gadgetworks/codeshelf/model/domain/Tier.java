/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Tier.java,v 1.15 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;
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

	// These two transient fields are not in database. Furthermore,
	// two different references to same object from the DAO may disagree on this field.
	@Transient
	@Column(nullable = true)
	@Getter
	@Setter
	private short					mTransientLedsThisTier;

	@Transient
	@Column(nullable = true)
	@Getter
	@Setter
	private boolean					mTransientLedsIncrease;

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
	
	private TierIds getTierIds() {
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
		// to support list view meta-field baySortName. Note: cannot sort by this string if more than 9 bays or 9 aisles.
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.bayName + "-" + theTierIds.tierName);
	}		

	public final String getTierSortName() {
		// to support list view meta-field tierSortName. Note: cannot sort by this string if more than 9 bays or 9 aisles.
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.tierName + "-" + theTierIds.bayName);
	}
	
	// converts A3 into 003.  Could put the A back on.
	private String getCompString(String inString) {
		String s = inString.substring(1); // Strip off the A, B, T, or S
		// we will pad with leading spaces to 3
		int padLength = 3;
		int needed = padLength - s.length();
		    if (needed <= 0) {
		      return s;
		    }
	    char[] padding = new char[needed];
	    java.util.Arrays.fill(padding, '0');
	    StringBuffer sb = new StringBuffer(padLength);
	    sb.append(padding);
	    sb.append(s);
	    String ss =  sb.toString();
	    return	ss;	
	}
	
	public final String getAisleTierBayForComparable() {
		// this is for a sort comparable.
		TierIds theTierIds = getTierIds();
		return (getCompString(theTierIds.aisleName) + "-" + getCompString(theTierIds.tierName) + "-" + getCompString(theTierIds.bayName));
	}

	
	public final String getAisleBayForComparable() {
		// this is for a sort comparable.
		TierIds theTierIds = getTierIds();
		return (getCompString(theTierIds.aisleName) + "-" + getCompString(theTierIds.bayName));
	}

	public final String getBayName() {
		// this is not for a sort comparable. Used in AislefilesCsvImporter, but could be used for meta field.
		TierIds theTierIds = getTierIds();
		return (theTierIds.aisleName + "-" + theTierIds.bayName);
	}

	public final void setControllerChannel(String inControllerPersistentIDStr, String inChannelStr, String inTiersStr) {
		// this is for callMethod from the UI
		// We are setting the controller and channel for the tier. Depending on the inTierStr parameter, may set also for
		// on all other same tier in the aisle, or perhaps other patterns.
		
		// Initially, log
		LOGGER.debug("Set tier controller to " + inControllerPersistentIDStr);
	}

}
