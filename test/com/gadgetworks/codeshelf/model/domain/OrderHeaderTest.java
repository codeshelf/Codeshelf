package com.gadgetworks.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;

public class OrderHeaderTest extends DomainTestABC {

	
	
	/**
	 * Given that some of the order locations have been inactivated
	 * When I Get First Location Along the Path
	 * Then I should not receive inactive locations 
	 */
	@SuppressWarnings("unused")
	@Test
	public void testReturnActiveLocationsOnly() {
		Facility facility = createFacility("ORG-testReturnActiveLocationsOnly");
		Aisle a1 = getAisle(facility, "A1");
		a1.setPickFaceEndPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 5.0, 0.0, 0.0));
		Aisle.DAO.store(a1);
		Aisle a2 = getAisle(facility, "A2");
		a2.setAnchorPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 10.0, 0.0)); //should have a move that moves
		a2.setPickFaceEndPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 5.0, 10.0, 0.0)); //should have a move that moves
		Aisle.DAO.store(a2);
		Path path = createAssociatedPath(a1, a2);

		a1 = Aisle.DAO.findByPersistentId(a1.getPersistentId());
		a2 = Aisle.DAO.findByPersistentId(a2.getPersistentId());
		
		Assert.assertNotNull(a1.getPosAlongPath());
		Assert.assertNotNull(a2.getPosAlongPath());
		Assert.assertTrue(a2.getPosAlongPath() > a1.getPosAlongPath());

		
		OrderHeader header = createOrderHeader("Order1", OrderTypeEnum.OUTBOUND, facility, null);
		OrderLocation loc1 = header.addOrderLocation(a1);
		OrderLocation loc2 = header.addOrderLocation(a2);
		
		OrderLocation orderLocation = header.getFirstOrderLocationOnPath(path);
		Assert.assertEquals(a1, orderLocation.getLocation());
		
		loc1.setActive(false);
		OrderLocation.DAO.store(loc1);
		
		OrderLocation orderLocationAfterInactivating = header.getFirstOrderLocationOnPath(path);
		Assert.assertEquals(a2, orderLocationAfterInactivating.getLocation());
		
		
		
		
		
		

	}

	private Path createAssociatedPath(Aisle a1, Aisle a2) {
		Facility facility = a1.getParentAtLevel(Facility.class);
		
		Path path = facility.createPath(a1.getDomainId() + ":" + a2.getDomainId());
		PathSegment segment = path.createPathSegment("a1->a2", 0, a1.getAnchorPoint(), a2.getAnchorPoint());
		a1.associatePathSegment(segment);
		a2.associatePathSegment(segment);
		return path;
	}
}
