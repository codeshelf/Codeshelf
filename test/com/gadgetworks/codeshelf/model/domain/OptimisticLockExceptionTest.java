package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

import lombok.Getter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.OrderDetail.OrderDetailDao;
import com.gadgetworks.codeshelf.model.domain.OrderHeader.OrderHeaderDao;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

public class OptimisticLockExceptionTest {

	@Getter
	PersistenceService persistenceService = PersistenceService.getInstance();

	@Before
	public final void setup() {
	}

	@Test
	public final void optimisticLockExceptionTest() {
		this.getPersistenceService().beginTenantTransaction();

		// EbeanServer defaultServer = Ebean.getServer(null);

		OrderHeader.DAO = new OrderHeaderDao(persistenceService);
		OrderDetail.DAO = new OrderDetailDao(persistenceService);
		Organization.DAO = new OrganizationDao(persistenceService);
		Facility.DAO = new FacilityDao(persistenceService);

		Organization.DAO = new OrganizationDao(persistenceService);
		Organization organization = new Organization();
		organization.setOrganizationId("OPTIMISTIC-O1");
		Organization.DAO.store(organization);

		Facility.DAO = new FacilityDao(persistenceService);
		Facility facility = new Facility();
		organization.addFacility(facility);
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

		this.getPersistenceService().endTenantTransaction();
	}
}
