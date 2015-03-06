package com.codeshelf.model.domain;

import static com.natpryce.makeiteasy.Property.newProperty;

import org.apache.commons.lang.RandomStringUtils;

import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.domain.Facility.FacilityDao;
import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;

public class DAOMaker {

	public DAOMaker() {
		Facility.setDao(new FacilityDao());
	}

	public static final Property<Facility, String>			facilityId			= newProperty();

	public final Instantiator<Facility>						TestFacility		= new Instantiator<Facility>() {
																					public Facility instantiate(PropertyLookup<Facility> lookup) {

																						Facility facility = Facility.createFacility(
																							lookup.valueOf(facilityId,
																							RandomStringUtils.randomAlphanumeric(5)),
																							lookup.valueOf(facilityId,
																								RandomStringUtils.randomAlphanumeric(5)),
																							new Point(PositionTypeEnum.GPS,
																								0.0d,
																								0.0d,
																								0.0d));
																						Facility.DAO.store(facility);
																						return facility;
																					}
																				};

}
