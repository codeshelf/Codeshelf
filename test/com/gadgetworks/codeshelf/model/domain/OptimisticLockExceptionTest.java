package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.OrderDetail.OrderDetailDao;
import com.gadgetworks.codeshelf.model.domain.OrderHeader.OrderHeaderDao;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;

public class OptimisticLockExceptionTest {
	
	private static IUtil			mUtil;
	private static ISchemaManager	mSchemaManager;
	private static IDatabase		mDatabase;

	@BeforeClass
	public final static void setup() {

		try {
			mUtil = new IUtil() {

				public void setLoggingLevelsFromPrefs(Organization inOrganization, ITypedDao<PersistentProperty> inPersistentPropertyDao) {
				}

				public String getVersionString() {
					return "";
				}

				public String getApplicationLogDirPath() {
					return ".";
				}

				public String getApplicationDataDirPath() {
					return ".";
				}

				public void exitSystem() {
					System.exit(-1);
				}
			};

			Class.forName("org.h2.Driver");
			mSchemaManager = new H2SchemaManager(mUtil, "codeshelf", "codeshelf", "codeshelf", "CODESHELF", "localhost", "");
			mDatabase = new Database(mSchemaManager, mUtil);

			mDatabase.start();
		} catch (ClassNotFoundException e) {
		}
	}

	@Test
	public final void optimisticLockExceptionTest() {

		EbeanServer defaultServer = Ebean.getServer(null);
		
		OrderHeader.DAO = new OrderHeaderDao();
		OrderDetail.DAO = new OrderDetailDao();
		Organization.DAO = new OrganizationDao();
		Facility.DAO = new FacilityDao();

		Organization organization = new Organization();
		organization.setOrganizationId("O1");
		Organization.DAO.store(organization);

		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setFacilityId("F1");
		facility.setPosType(PositionTypeEnum.METERS_FROM_DATUM);
		facility.setPosX(0.0);
		facility.setPosY(0.0);
		Facility.DAO.store(facility);
		
		OrderHeader order1 = new OrderHeader();
		order1.setDomainId("123");
		order1.setParent(facility);
		order1.setStatusEnum(OrderStatusEnum.CREATED);
		order1.setPickStrategyEnum(PickStrategyEnum.SERIAL);
		OrderHeader.DAO.store(order1);

		OrderHeader foundOrder = OrderHeader.DAO.findByDomainId(facility, "123");
		foundOrder.setStatusEnum(OrderStatusEnum.INPROGRESS);
		OrderHeader.DAO.store(foundOrder);
		
		order1.setStatusEnum(OrderStatusEnum.COMPLETE);
		order1.setVersion(new Timestamp(order1.getVersion().getTime() + 1));
		try {
			OrderHeader.DAO.store(order1);
		} catch (RuntimeException e) {
			// So we should come here with an optimistic lock exception.
			Assert.fail("DAO Exception");
		}
		
		foundOrder = OrderHeader.DAO.findByDomainId(facility, "123");
		Assert.assertEquals(foundOrder.getStatusEnum(), order1.getStatusEnum());
	}
}
