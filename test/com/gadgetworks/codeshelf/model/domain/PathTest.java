/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;

/**
 * @author jeffw
 *
 */
public class PathTest extends DomainTestABC {

	@Test
	public final void addRemoveOrderGroupTest() {

		Organization organization = new Organization();
		organization.setOrganizationId("O-PATH1");
		mOrganizationDao.store(organization);

		organization.createFacility("F1", "test", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F1");

		Aisle aisle1 = new Aisle(facility, "A1", 0.0, 0.0);
		mAisleDao.store(aisle1);

		Bay baya1b1 = new Bay(aisle1, "B1", 0.0, 0.0, 0.0);
		mBayDao.store(baya1b1);
		Bay baya1b2 = new Bay(aisle1, "B2", 0.0, 0.0, 0.0);
		mBayDao.store(baya1b2);

		Aisle aisle2 = new Aisle(facility, "A2", 0.0, 0.0);
		mAisleDao.store(aisle2);

		Bay baya2b1 = new Bay(aisle2, "B1", 0.0, 0.0, 0.0);
		mBayDao.store(baya2b1);
		Bay baya2b2 = new Bay(aisle2, "B2", 0.0, 0.0, 0.0);
		mBayDao.store(baya2b2);

		Path path = new Path();
		path.setDomainId(Path.DEFAULT_FACILITY_PATH_ID);
		path.setParent(facility);
		path.setTravelDirEnum(TravelDirectionEnum.FORWARD);
		mPathDao.store(path);
		facility.addPath(path);

		PathSegment pathSegment1 = path.createPathSegment("PS1", aisle1, path, 0, new Point(PositionTypeEnum.METERS_FROM_PARENT,
			0.0,
			0.0,
			0.0), new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, 0.0));
		mPathSegmentDao.store(pathSegment1);

		pathSegment1.addLocation(aisle2);
		aisle1.setPathSegment(pathSegment1);
		mAisleDao.store(aisle2);

		Aisle aisle3 = new Aisle(facility, "A3", 0.0, 0.0);
		mAisleDao.store(aisle3);

		Bay baya3b1 = new Bay(aisle3, "B1", 0.0, 0.0, 0.0);
		mBayDao.store(baya3b1);
		Bay baya3b2 = new Bay(aisle3, "B2", 0.0, 0.0, 0.0);
		mBayDao.store(baya3b2);

		Aisle aisle4 = new Aisle(facility, "A4", 0.0, 0.0);
		mAisleDao.store(aisle4);

		Bay baya4b1 = new Bay(aisle4, "B1", 0.0, 0.0, 0.0);
		mBayDao.store(baya4b1);
		Bay baya4b2 = new Bay(aisle4, "B2", 0.0, 0.0, 0.0);
		mBayDao.store(baya4b2);

		PathSegment pathSegment2 = path.createPathSegment("PS2", aisle3, path, 1, new Point(PositionTypeEnum.METERS_FROM_PARENT,
			0.0,
			0.0,
			0.0), new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, 0.0));
		mPathSegmentDao.store(pathSegment2);

		pathSegment2.addLocation(aisle4);
		aisle1.setPathSegment(pathSegment2);
		mAisleDao.store(aisle4);

		facility.logLocationDistances();
		
		// Check if we can find all four aisles.
		List<Aisle> aisleList = path.<Aisle> getLocationsByClass(Aisle.class);
		Assert.assertEquals(4, aisleList.size());

		// Check if we can find all eight bays.
		List<Bay> bayList = path.<Bay> getLocationsByClass(Bay.class);
		Assert.assertEquals(8, bayList.size());

		// Make sure we don't find other random locations.
		List<Tier> tierList = path.<Tier> getLocationsByClass(Tier.class);
		Assert.assertEquals(0, tierList.size());

	}
}
