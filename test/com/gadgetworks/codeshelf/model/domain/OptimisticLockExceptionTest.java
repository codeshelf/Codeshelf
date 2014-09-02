package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.OrderDetail.OrderDetailDao;
import com.gadgetworks.codeshelf.model.domain.OrderHeader.OrderHeaderDao;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.platform.services.PersistencyService;

public class OptimisticLockExceptionTest {

	PersistencyService persistencyService = new PersistencyService();

	@Before
	public final void setup() {
	}

	@Test
	public final void optimisticLockExceptionTest() {

		// EbeanServer defaultServer = Ebean.getServer(null);

		OrderHeader.DAO = new OrderHeaderDao(persistencyService);
		OrderDetail.DAO = new OrderDetailDao(persistencyService);
		Organization.DAO = new OrganizationDao(persistencyService);
		Facility.DAO = new FacilityDao(persistencyService);

		Organization.DAO = new OrganizationDao(persistencyService);
		Organization organization = new Organization();
		organization.setOrganizationId("OPTIMISTIC-O1");
		Organization.DAO.store(organization);

		Facility.DAO = new FacilityDao(persistencyService);
		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setFacilityId("OPTIMISTIC-F1");
		facility.setAnchorPoint(new Point(PositionTypeEnum.GPS, 0.0, 0.0, 0.0));
		Facility.DAO.store(facility);

		OrderHeader order1 = new OrderHeader();
		order1.setDomainId("OPTIMISTIC-123");
		order1.setParent(facility);
		order1.setOrderTypeEnum(OrderTypeEnum.OUTBOUND);
		order1.setStatusEnum(OrderStatusEnum.CREATED);
		order1.setPickStrategyEnum(PickStrategyEnum.SERIAL);
		order1.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1.setActive(true);
		order1.setUpdated(new Timestamp(System.currentTimeMillis()));
		OrderHeader.DAO.store(order1);

		OrderHeader foundOrder = OrderHeader.DAO.findByDomainId(facility, "OPTIMISTIC-123");
		foundOrder.setStatusEnum(OrderStatusEnum.INPROGRESS);
		OrderHeader.DAO.store(foundOrder);

		order1.setStatusEnum(OrderStatusEnum.COMPLETE);
		order1.setVersion(new Timestamp(order1.getVersion().getTime() + 1));
		OrderHeader.DAO.store(order1);

		foundOrder = OrderHeader.DAO.findByDomainId(facility, "OPTIMISTIC-123");
		Assert.assertEquals(foundOrder.getStatusEnum(), order1.getStatusEnum());
	}
}
