package com.gadgetworks.codeshelf.model.domain;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static com.natpryce.makeiteasy.Property.newProperty;

import org.apache.commons.lang.RandomStringUtils;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;

public class DAOMaker {

	public DAOMaker(ISchemaManager mSchemaManager) {
		Organization.setDAO(new OrganizationDao(mSchemaManager));
		Facility.setDAO(new FacilityDao(mSchemaManager));

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
	        @SuppressWarnings("unchecked")
			Facility facility = new Facility(
	        	lookup.valueOf(organization, make(a(TestOrganization))),
	        	lookup.valueOf(facilityId, RandomStringUtils.randomAlphanumeric(5)),
	        	new Point(PositionTypeEnum.GPS, 0.0d, 0.0d, 0.0d));
	    	Facility.DAO.store(facility);
	        return facility;
	    }
	};

}
