package com.codeshelf.model;

import java.sql.Timestamp;
import java.util.Collections;
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
	public void testPurgeSomeOrdersWithEmpty() {
		beginTransaction();
		Facility facility = getFacility();
		DomainObjectManager subject = new DomainObjectManager(facility);
		subject.purgeSomeOrders(Collections.<UUID>emptyList());
		commitTransaction();
	}

	@Test
	public void testPurgeOrdersWithNoDetails() {
		beginTransaction();
		Facility facility = getFacility();
		OrderHeader oh = createOrderHeader("1", OrderTypeEnum.OUTBOUND,  facility,  null);
		
		oh.setDueDate(new Timestamp(new DateTime().minusDays(2).getMillis()));
		DomainObjectManager subject = new DomainObjectManager(facility);
		subject.purgeOldObjects(1, OrderHeader.class,  5);
		commitTransaction();

		beginTransaction();
		Assert.assertNull(OrderHeader.staticGetDao().findByPersistentId(oh.getPersistentId()));
		commitTransaction();

	}

}
