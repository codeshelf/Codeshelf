/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.5 2012/02/21 02:45:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;

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
@Table(name = "FACILITY")
public class Facility extends PersistABC {

	public static final GenericDao<Facility>	DAO					= new GenericDao<Facility>(Facility.class);

	private static final Log					LOGGER				= LogFactory.getLog(Facility.class);

	private static final long					serialVersionUID	= 3001609308065821464L;

	// The facility description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String								description;

	// The owning facility.
	@Column(name = "parentOrganization", nullable = false)
	@ManyToOne
	private Organization						parentOrganization;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentFacility")
	private List<Aisle>							aisles				= new ArrayList<Aisle>();

	public Facility() {
		description = "";
	}
	
	public final Organization getParentOrganization() {
		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
		if (parentOrganization != null) {
			parentOrganization = Organization.DAO.loadByPersistentId(parentOrganization.getPersistentId());
		}
		return parentOrganization;
	}

	public final void setparentOrganization(Organization inparentOrganization) {
		parentOrganization = inparentOrganization;
	}

	// We always need to return the object cached in the DAO.
	public final List<Aisle> getAisless() {
		if (IGenericDao.USE_DAO_CACHE) {
			List<Aisle> result = new ArrayList<Aisle>();
			if (!Aisle.DAO.isObjectPersisted(this)) {
				result = aisles;
			} else {
				for (Aisle aisle : Aisle.DAO.getAll()) {
					if (aisle.getParentFacility().equals(this)) {
						result.add(aisle);
					}
				}
			}
			return result;
		} else {
			return aisles;
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addControlGroup(Aisle inAisle) {
		aisles.add(inAisle);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeControlGroup(Aisle inAisle) {
		aisles.remove(inAisle);
	}
}
