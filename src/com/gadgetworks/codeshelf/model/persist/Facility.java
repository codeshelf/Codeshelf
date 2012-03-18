/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.9 2012/03/18 04:12:26 jeffw Exp $
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
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 * Facility
 * 
 * The basic unit that holds all of the locations and equipment for a single facility in an organization.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "FACILITY")
public class Facility extends PersistABC {

	public interface IFacilityDao extends IGenericDao<Facility> {		
	}
	
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
	@JsonIgnore
	@Getter
	private Organization						parentOrganization;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentFacility")
	@JsonIgnore
	@Getter
	private List<Aisle>							aisles				= new ArrayList<Aisle>();

	public Facility() {
		description = "";
	}
	
//	public final Organization getParentOrganization() {
//		// Yes, this is weird, but we MUST always return the same instance of these persistent objects.
//		if (parentOrganization != null) {
//			OrganizationDao dao = new OrganizationDao();
//			parentOrganization = dao.loadByPersistentId(parentOrganization.getPersistentId());
//		}
//		return parentOrganization;
//	}

	public final void setparentOrganization(Organization inparentOrganization) {
		parentOrganization = inparentOrganization;
	}
	
	public final String getParentOrganizationID() {
		return getParentOrganization().getId();
	}

//	// We always need to return the object cached in the DAO.
//	public final List<Aisle> getAisles() {
//		if (IGenericDao.USE_DAO_CACHE) {
//			List<Aisle> result = new ArrayList<Aisle>();
//			AisleDao aisleDao = new AisleDao();
//			if (!aisleDao.isObjectPersisted(this)) {
//				result = aisles;
//			} else {
//				for (Aisle aisle : aisleDao.getAll()) {
//					if (aisle.getParentFacility().equals(this)) {
//						result.add(aisle);
//					}
//				}
//			}
//			return result;
//		} else {
//			return aisles;
//		}
//	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addAisle(Aisle inAisle) {
		aisles.add(inAisle);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeAisle(Aisle inAisle) {
		aisles.remove(inAisle);
	}
}
