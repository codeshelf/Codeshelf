package com.codeshelf.model.domain;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.testframework.HibernateTest;
import com.google.common.collect.ImmutableMap;

public class OrderHeaderTest extends HibernateTest {

	
	
	/**
	 * Given that some of the order locations have been inactivated
	 * When I Get First Location Along the Path
	 * Then I should not receive inactive locations 
	 */
	@SuppressWarnings("unused")
	@Test
	public void testReturnActiveLocationsOnly() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = createFacility();
		Aisle a1 = getDefaultAisle(facility, "A1");
		a1.setPickFaceEndPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 5.0, 0.0, 0.0));
		Aisle.staticGetDao().store(a1);
		Aisle a2 = getDefaultAisle(facility, "A2");
		a2.setAnchorPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 10.0, 0.0)); //should have a move that moves
		a2.setPickFaceEndPoint(new Point(PositionTypeEnum.METERS_FROM_PARENT, 5.0, 10.0, 0.0)); //should have a move that moves
		Aisle.staticGetDao().store(a2);
		Path path = createAssociatedPath(a1, a2);

		a1 = Aisle.staticGetDao().findByPersistentId(a1.getPersistentId());
		a2 = Aisle.staticGetDao().findByPersistentId(a2.getPersistentId());
		
		Assert.assertNotNull(a1.getPosAlongPath());
		Assert.assertNotNull(a2.getPosAlongPath());
		Assert.assertTrue(a2.getPosAlongPath() > a1.getPosAlongPath());

		
		OrderHeader header = createOrderHeader("Order1", OrderTypeEnum.OUTBOUND, facility, null);
		OrderLocation loc1 = header.addOrderLocation(a1);
		OrderLocation loc2 = header.addOrderLocation(a2);
		
		OrderLocation orderLocation = header.getFirstOrderLocationOnPath(path);
		Assert.assertEquals(a1, orderLocation.getLocation());
		
		loc1.setActive(false);
		OrderLocation.staticGetDao().store(loc1);
		
		OrderLocation orderLocationAfterInactivating = header.getFirstOrderLocationOnPath(path);
		Assert.assertEquals(a2, orderLocationAfterInactivating.getLocation());
		
		
		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public void testOrderHeaderByFacilityAndType() {
		this.getTenantPersistenceService().beginTransaction();
		OrderHeader.staticGetDao().findByFilter("orderHeadersByFacilityAndType",
											ImmutableMap.<String, Object>of(
												"facilityId", UUID.randomUUID(),
												"orderType", OrderTypeEnum.OUTBOUND.name()));
		this.getTenantPersistenceService().commitTransaction();
	}
	
	private Path createAssociatedPath(Aisle a1, Aisle a2) {
		Facility facility = a1.getParentAtLevel(Facility.class);
		
		Path path = facility.createPath(a1.getDomainId() + ":" + a2.getDomainId());
		PathSegment segment = path.createPathSegment(0, a1.getAnchorPoint(), a2.getAnchorPoint());
		a1.associatePathSegment(segment);
		a2.associatePathSegment(segment);
		return path;
	}
}
