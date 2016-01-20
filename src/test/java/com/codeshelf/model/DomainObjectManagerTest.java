package com.codeshelf.model;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.scheduler.DataPurgeJob;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.util.TimeUtils;

public class DomainObjectManagerTest extends HibernateTest {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(DomainObjectManagerTest.class);
	
	/**
	 * This used to fail under postgres with a SQL syntax exception like WHERE X IN ()
	 * But not under H2
	 */
	@Test
	public void testSomePurgesWithEmptyList() {
		beginTransaction();
		Facility facility = getFacility();
		DomainObjectManager subject = new DomainObjectManager(facility);
		
		Assert.assertEquals(0, subject.purgeSomeOrders(Collections.<UUID>emptyList()));
		Assert.assertEquals(0, subject.purgeSomeWiCsvBeans(Collections.<UUID>emptyList()));
		Assert.assertEquals(0, subject.purgeSomeWorkInstructions(Collections.<UUID>emptyList()));
		Assert.assertEquals(0, subject.purgeSomeWorkerEvents(Collections.<UUID>emptyList()));
		Assert.assertEquals(0, subject.purgeSomeCntrs(Collections.<UUID>emptyList()));
		Assert.assertEquals(0, subject.purgeSomeImportReceipts(Collections.<UUID>emptyList()));
		Assert.assertEquals(0, subject.purgeSomeExportMessages(Collections.<UUID>emptyList()));
			
		commitTransaction();
	}
	
	/**
	 * This barely tests execution
	 * Hopefully, will fail if the query is bad, even if the table is empty.
	 * Note: did fail as desired for JR on Mac during development
	 */
	@Test
	public void testClassListQueries() {
		beginTransaction();
		Facility facility = getFacility();
		DomainObjectManager subject = new DomainObjectManager(facility);
		
		Assert.assertEquals(0, subject.getOrderUuidsToPurge(1).size());
		Assert.assertEquals(0, subject.getWorkerEventUuidsToPurge(1).size());
		Assert.assertEquals(0, subject.getWorkInstructionCsvBeanUuidsToPurge(1).size());
		Assert.assertEquals(0, subject.getExportMessageUuidsToPurge(1).size());
		Assert.assertEquals(0, subject.getImportReceiptUuidsToPurge(1).size());
		Assert.assertEquals(0, subject.getWorkInstructionUuidsToPurge(1).size());
		Assert.assertEquals(0, subject.getCntrUuidsToPurge(1).size());
			
		commitTransaction();
	}


	@Test
	public void testPurgeOrdersWithNoDetails() {
		// Also minimally covers getOrderUuidsToPurge() and reportAchiveables()

		beginTransaction();
		Facility facility = getFacility();
		
		OrderHeader oh = createOrderHeader("1", OrderTypeEnum.OUTBOUND,  facility,  null);		
		oh.setDueDate(new Timestamp(new DateTime().minusDays(2).getMillis()));
		UUID ohId = oh.getPersistentId();
		commitTransaction();

		beginTransaction();	
		DomainObjectManager subject = new DomainObjectManager(facility);
		subject.reportAchiveables(1);
		List<UUID> ohIds = subject.getOrderUuidsToPurge(1);
		Assert.assertEquals(1, subject.purgeSomeOrders(ohIds));
		commitTransaction();

		beginTransaction();
		Assert.assertNull(OrderHeader.staticGetDao().findByPersistentId(ohId));		
		commitTransaction();
	}
	
	@Test
	public void testPurgeReplenishOrderDetails() throws Exception{
		beginTransaction();
		Facility facility = getFacility();
		commitTransaction();
		
		beginTransaction();
		LOGGER.info("1: Create 1 replenish order with 3 details");
		facility = facility.reload();
		String ordersCsv = "orderId,itemId,quantity,uom,locationId,preAssignedContainerId,workSequence,operationType\n" + 
				"gtin1,Item1,1,each,LocX26,gtin1,0,replenish\n" + 
				"gtin1,Item1,1,each,LocX27,gtin1,0,replenish\n" + 
				"gtin1,Item1,1,each,LocX28,gtin1,0,replenish\n";
		importOrdersData(facility, ordersCsv);
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "gtin1");
		Assert.assertNotNull(order);
		List<OrderDetail> details = order.getOrderDetails();
		Assert.assertEquals(3, details.size());
		LOGGER.info("2: Make 2 details older, so that they fit the purge params");
		details.get(0).setUpdated(new Timestamp(System.currentTimeMillis() - 30 * TimeUtils.MILLISECOUNDS_IN_DAY));
		details.get(1).setUpdated(new Timestamp(System.currentTimeMillis() - 15 * TimeUtils.MILLISECOUNDS_IN_DAY));
		details.get(2).setUpdated(new Timestamp(System.currentTimeMillis() - 25 * TimeUtils.MILLISECOUNDS_IN_DAY));
		commitTransaction();
		
		LOGGER.info("3: Run the purge");
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		UserContext userContexr = CodeshelfSecurityManager.getCurrentUserContext();
		DataPurgeJob purgeJob = new DataPurgeJob();
		purgeJob.doFacilityExecute(tenant, facility);
		CodeshelfSecurityManager.setContext(userContexr, tenant);	//Running the DataPurgeJob above disrupts Tenant setting
		
		LOGGER.info("4: Ensure that only 1 detail remains");
		beginTransaction();
		order = OrderHeader.staticGetDao().findByDomainId(facility, "gtin1");
		Assert.assertNotNull(order);
		details = order.getOrderDetails();
		Assert.assertEquals(1, details.size());
		commitTransaction();
	}

}
