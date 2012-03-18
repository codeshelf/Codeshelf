/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Aisle.java,v 1.7 2012/03/18 04:12:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.google.inject.Inject;

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

	public interface IAisleDao extends IGenericDao<Aisle> {		
	}
	
	private static final Log				LOGGER				= LogFactory.getLog(Aisle.class);

	// The owning facility.
	@Column(name = "parentFacility", nullable = false)
	@ManyToOne
	@Getter
	@Setter
	private Facility						parentFacility;

	public Aisle() {
		parentFacility = null;
	}

//	public final Facility getParentFacility() {
//		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
//		if (parentFacility != null) {
//			FacilityDao dao = new FacilityDao();
//			parentFacility = dao.loadByPersistentId(parentFacility.getPersistentId());
//		}
//		return parentFacility;
//	}

//	public final void setParentFacility(Facility inParentFacility) {
//		parentFacility = inParentFacility;
//	}
}
