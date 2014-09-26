package com.gadgetworks.codeshelf.generators;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IronMqService;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;

public class FacilityGenerator {
	
	public Facility generateValid() {
		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		Organization.DAO.store(organization);
		Facility facility = organization.createFacility("F1", "", Point.getZeroPoint());
		facility.addEdiService(new IronMqService());
		return facility;
	}
}
