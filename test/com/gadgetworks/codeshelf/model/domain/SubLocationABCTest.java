package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class SubLocationABCTest extends DomainTestABC {

	//@Test
	public void computePositionAlongPath() {
		Facility facility = createFacility(this.getClass().toString());

		Point origin = Point.getZeroPoint().add(1.232234234234, 13.11111111111);
		Point pickFace = origin.add(6.1111111111111111111, 0.0);
		Aisle aisle = getAisle(facility, "A1", origin, pickFace);
		
		Point bayOrigin = Point.getZeroPoint();
		List<Bay> bays = new ArrayList<Bay>();
		for (int i = 0; i < 5; i++) {
			Point anchor = bayOrigin.add(i*1.2222222222222222222222, 0.0);
			Point pickEnd = anchor.add(i+1*1.2222222222222222222222, 0.0);
			bays.add(new Bay(aisle, "B"+(i+1), anchor, pickEnd));
		}
		Path path = facility.createPath("P1");
		PathSegment inPathSegment = path.createPathSegment("P1.0", 0, pickFace.add(1, 5.0), origin.add(-1, 5.0));
		aisle.associatePathSegment(inPathSegment);
		
		
		Assert.assertEquals(1, facility.getPaths().size());
		List<ISubLocation<?>> bayList = new ArrayList<ISubLocation<?>>();
		for (Path facilityPath : facility.getPaths()) {
			
			bayList.addAll(facilityPath.<ISubLocation<?>> getLocationsByClass(Bay.class));
		}
		Assert.assertEquals(bays.size(), bayList.size());
		// Cycle over all bays on the path.
		for (ISubLocation<?> subLocation : bayList) {
			System.out.println(subLocation + "," + subLocation.getPosAlongPath());
		}
				
	}
}
