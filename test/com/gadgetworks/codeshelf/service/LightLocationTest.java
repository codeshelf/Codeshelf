package com.gadgetworks.codeshelf.service;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdPath;
import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.flyweight.command.ColorEnum;

public class LightLocationTest extends DomainTestABC {

	@Test
	public void testLightAisle() {
		for (Aisle aisle : generateAisles()) {
			List<LedCmdGroup> ledCommands = aisle.getLedsToCheck(ColorEnum.RED);

			//for all combinations of controller|channel, bay at least one led position from list is within the bay

			Set<LedCmdPath> allControllerAndChannelCombos = aisle.getAllLedCmdPaths();
			List<ISubLocation> allChildren = aisle.getChildren(); 

			Assert.assertEquals(allChildren.size() * allControllerAndChannelCombos.size(), ledCommands.size());
		}
	}
	
	

	private List<Aisle> generateAisles() {
		Facility facility = createFacilityWithOutboundOrders("ORG");
		Facility facility2 = setUpSimpleNoSlotFacility("ORG2");
		ArrayList<Aisle> aisles = new ArrayList<Aisle>();
		aisles.addAll(facility.getDao().findByPersistentId(facility.getPersistentId()).getAisles());
		aisles.addAll(facility2.getDao().findByPersistentId(facility2.getPersistentId()).getAisles());
		return aisles;
	}
	
	private static Bay generateBay(Aisle aisle) {
		return new Bay(aisle, generateString(), generatePoint(), generatePoint());
	}

	private static Point generatePoint() {
		return new Point(PositionTypeEnum.METERS_FROM_PARENT, generateDouble(), generateDouble(), generateDouble());
	}

	private static int generatePositiveInt(int min, int max) {
		Random r = new Random();
		return r.nextInt(max - min) + min;
	}
	
	private static double generateDouble() {
		return Math.random()*10;
	}

	private static Facility generateFacility() {
		return new Facility(generateOrganization(), generateString(), generatePoint());
	}

	private static String generateString() {
		return UUID.randomUUID().toString();
	}

	private static Organization generateOrganization() {
		return new Organization(generateString());
	}

	
}
