package com.codeshelf.model.domain;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PickStrategyEnum;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.testframework.MockDaoTest;

public class OptimisticLockExceptionTest extends MockDaoTest {
	@Test
	public final void optimisticLockExceptionTest() {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = new Facility();
		facility.setFacilityId("OPTIMISTIC-F1");
		facility.setAnchorPoint(new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		Facility.DAO.store(facility);

		OrderHeader order1 = new OrderHeader();
		order1.setDomainId("OPTIMISTIC-123");
		order1.setParent(facility);
		order1.setOrderType(OrderTypeEnum.OUTBOUND);
		order1.setStatus(OrderStatusEnum.CREATED);
		order1.setPickStrategy(PickStrategyEnum.SERIAL);
		order1.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1.setActive(true);
		order1.setUpdated(new Timestamp(System.currentTimeMillis()));
		OrderHeader.DAO.store(order1);

		OrderHeader foundOrder = OrderHeader.DAO.findByDomainId(facility, "OPTIMISTIC-123");
		foundOrder.setStatus(OrderStatusEnum.INPROGRESS);
		OrderHeader.DAO.store(foundOrder);

		order1.setStatus(OrderStatusEnum.COMPLETE);
		order1.setVersion(order1.getVersion() + 1);
		OrderHeader.DAO.store(order1);

		foundOrder = OrderHeader.DAO.findByDomainId(facility, "OPTIMISTIC-123");
		Assert.assertEquals(foundOrder.getStatus(), order1.getStatus());

		this.getTenantPersistenceService().commitTransaction();
	}
}
