/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Organization.java,v 1.5 2012/03/22 06:58:44 jeffw Exp $
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
import org.codehaus.jackson.annotate.JsonIgnore;

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
@Table(name = "ORGANIZATION")
public class Organization extends PersistABC {

//	public interface IOrganizationDao extends IGenericDao<Organization> {		
//	}

	private static final Log						LOGGER		= LogFactory.getLog(Organization.class);

	// The facility description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String									description;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentOrganization")
	@JsonIgnore
	@Getter
	private List<Facility>							facilities	= new ArrayList<Facility>();

	public Organization() {
		description = "";
	}

//	// We always need to return the object cached in the DAO.
//	public final List<Facility> getFacilities() {
//		if (IGenericDao.USE_DAO_CACHE) {
//			List<Facility> result = new ArrayList<Facility>();
//			OrganizationDao organizationDao = new OrganizationDao();
//			if (!organizationDao.isObjectPersisted(this)) {
//				result = facilities;
//			} else {
//				FacilityDao facilityDao = new FacilityDao();
//				for (Facility facility : facilityDao.getAll()) {
//					if (facility.getParentOrganization().equals(this)) {
//						result.add(facility);
//					}
//				}
//			}
//			return result;
//		} else {
//			return facilities;
//		}
//	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addControlGroup(Facility inFacility) {
		facilities.add(inFacility);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeControlGroup(Facility inFacility) {
		facilities.remove(inFacility);
	}
}
