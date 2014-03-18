package com.gadgetworks.codeshelf.model.domain;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.Property.newProperty;

import org.apache.commons.lang.RandomStringUtils;

import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;

public class DAOMaker {

	public DAOMaker(ISchemaManager mSchemaManager) {
		Organization.DAO = new OrganizationDao(mSchemaManager);
		Facility.DAO = new FacilityDao(mSchemaManager);
		
	}
	
	public static final Property<Organization,String> organizationId = newProperty();
	
	public static final Property<Facility,String> facilityId = newProperty();
	public static final Property<Facility,Organization> organization  = newProperty();
	
	public final Instantiator<Organization> TestOrganization = new Instantiator<Organization>() {
	    public Organization instantiate(PropertyLookup<Organization> lookup) {

	    	
	    	Organization organization = new Organization();
	    	organization.setOrganizationId(lookup.valueOf(organizationId, RandomStringUtils.randomAlphanumeric(5)));
			Organization.DAO.store(organization);
	        return organization;
	    }
	};
	
	public final Instantiator<Facility> TestFacility = new Instantiator<Facility>() {
	    public Facility instantiate(PropertyLookup<Facility> lookup) {
	        Facility facility = new Facility(0.0, 0.0);
	        facility.setFacilityId(lookup.valueOf(facilityId, RandomStringUtils.randomAlphanumeric(5)));
	    	facility.setParent(lookup.valueOf(organization, make(a(TestOrganization))));
	    	Facility.DAO.store(facility);
	        return facility;
	    }
	};
	
}