package com.codeshelf.ws.protocol.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FacilityProperty;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.ws.protocol.request.FacilityPropertiesRequest;
import com.codeshelf.ws.protocol.response.FacilityPropertiesResponse;
import com.codeshelf.ws.protocol.response.ResponseStatus;

public class ObjectPropertyCommandTest extends HibernateTest {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ObjectPropertyCommandTest.class);

	@Test
	public void testObjectPropertyCommandUsingDefault() {
		beginTransaction();

		Facility facility=createFacility();
		FacilityPropertyType type = FacilityPropertyType.TIMEZONE;
		PropertyBehavior.setProperty(facility, type, "US/EASTERN");

		FacilityPropertiesRequest req =  new FacilityPropertiesRequest();
		req.setPersistentId(facility.getPersistentId().toString());
		FacilityPropertiesCommand command = new FacilityPropertiesCommand(null, req);
		FacilityPropertiesResponse resp = (FacilityPropertiesResponse) command.exec();
		
		assertNotNull(resp);
		assertEquals(ResponseStatus.Success, resp.getStatus());
		List<FacilityProperty> results = resp.getResults();
		assertNotNull(results);
		boolean timezoneCorrect = false;
		for (FacilityProperty property : results) {
			if (type.name().equals(property.getName()) && "US/EASTERN".equalsIgnoreCase(property.getValue())){
				timezoneCorrect = true;
			}
		}
		Assert.assertTrue(timezoneCorrect);
		
		commitTransaction();
	}	
}
