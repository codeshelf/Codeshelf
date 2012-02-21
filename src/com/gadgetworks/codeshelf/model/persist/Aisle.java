/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.5 2012/02/21 02:45:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.dao.GenericDao;

// --------------------------------------------------------------------------
/**
 * CodeShelfNetwork
 * 
 * The CodeShelfNetwork object holds information about how to create a standalone CodeShelf network.
 * (There may be more than one running at a facility.)
 * 
 * @author jeffw
 */

@Entity
@Table(name = "AISLE")
public class Aisle extends PersistABC {

	public static final GenericDao<Aisle>	DAO					= new GenericDao<Aisle>(Aisle.class);

	private static final Log				LOGGER				= LogFactory.getLog(Aisle.class);

	// The owning facility.
	@Column(name = "parentFacility", nullable = false)
	@ManyToOne
	private Facility						parentFacility;

	public Aisle() {
		parentFacility = null;
	}

	public final Facility getParentFacility() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (parentFacility != null) {
			parentFacility = Facility.DAO.loadByPersistentId(parentFacility.getPersistentId());
		}
		return parentFacility;
	}

	public final void setParentFacility(Facility inParentFacility) {
		parentFacility = inParentFacility;
	}
}
