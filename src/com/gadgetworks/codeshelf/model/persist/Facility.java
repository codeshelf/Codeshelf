/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.3 2011/12/29 09:15:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
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

	private static final Log					LOGGER				= LogFactory.getLog(CodeShelfNetwork.class);

	private static final long					serialVersionUID	= 3001609308065821464L;

	// The facility description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String								description;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentFacility")
	private List<Aisle>							aisles				= new ArrayList<Aisle>();

	public Facility() {
		description = "";
	}

	//	public  String toString() {
	//		return getId() + " " + mDescription;
	//	}
	//
	//	public  String getDescription() {
	//		return mDescription;
	//	}
	//
	//	public  void setDescription(String inDescription) {
	//		mDescription = inDescription;
	//	}

	// We always need to return the object cached in the DAO.
	public List<Aisle> getControlGroups() {
		if (IGenericDao.USE_DAO_CACHE) {
			List<Aisle> result = new ArrayList<Aisle>();
			if (!ControlGroup.DAO.isObjectPersisted(this)) {
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
	public void addControlGroup(Aisle inAisle) {
		aisles.add(inAisle);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public void removeControlGroup(Aisle inAisle) {
		aisles.remove(inAisle);
	}
}
