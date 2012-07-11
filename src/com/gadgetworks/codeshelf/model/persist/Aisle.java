/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.15 2012/07/11 07:15:42 jeffw Exp $
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

// --------------------------------------------------------------------------
/**
 * Aisle
 * 
 * Aisle is a facility-level location that holds a collection of bays.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "AISLE")
@DiscriminatorValue("AISLE")
public class Aisle extends Location {

	private static final Log	LOGGER	= LogFactory.getLog(Aisle.class);
	
	@Inject
	public static ITypedDao<Aisle> DAO;
//	public static final ITypedDao<Aisle> DAO  = new GenericDao<Aisle>(Aisle.class);

	public Aisle() {

	}
	
	public final PersistABC getParent() {
		return getParentLocation();
	}
	
	public final void setParent(PersistABC inParent) {
		if (inParent instanceof Facility) {
			setParentLocation((Facility) inParent);
		}
	}
	
	public final Facility getParentFacility() {
		return (Facility) getParentLocation();
	}
	
	public final void setParentFacility(Facility inParentFacility) {
		setParentLocation(inParentFacility);
	}
}
