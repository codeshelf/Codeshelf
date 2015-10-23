package com.codeshelf.model;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.testframework.HibernateTest;

public class DomainObjectManagerTest extends HibernateTest {

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
		// Also minimally covers getOrderUuidsToPurge();

		beginTransaction();
		Facility facility = getFacility();
		
		OrderHeader oh = createOrderHeader("1", OrderTypeEnum.OUTBOUND,  facility,  null);		
		oh.setDueDate(new Timestamp(new DateTime().minusDays(2).getMillis()));
		UUID ohId = oh.getPersistentId();
		commitTransaction();

		beginTransaction();	
		DomainObjectManager subject = new DomainObjectManager(facility);
		List<UUID> ohIds = subject.getOrderUuidsToPurge(1);
		Assert.assertEquals(1, subject.purgeSomeOrders(ohIds));
		commitTransaction();

		beginTransaction();
		Assert.assertNull(OrderHeader.staticGetDao().findByPersistentId(ohId));
		commitTransaction();

	}

}
