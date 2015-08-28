package com.codeshelf.model.dao;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.testframework.HibernateTest;

public class DomainObjectCacheTest extends HibernateTest {

	@Test
	public void testReturnActiveLocationsOnly() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = this.createFacility();
		
		DomainObjectCache<ItemMaster> cache = new DomainObjectCache<ItemMaster>(ItemMaster.staticGetDao(), "ItemMaster", facility);
		cache.loadAll();
		Assert.assertEquals(0, cache.size());
		
		UomMaster uomMaster = new UomMaster();
		uomMaster.setUomMasterId("EA");
		uomMaster.setParent(facility);
		UomMaster.staticGetDao().store(uomMaster);
		facility.addUomMaster(uomMaster);
		ItemMaster im = new ItemMaster(facility, "IM#1", uomMaster);
		ItemMaster.staticGetDao().store(im);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		cache.loadAll();
		Assert.assertEquals(1, cache.size());
		ItemMaster im2 = new ItemMaster();
		im2.setParent(facility);
		im2.setDomainId("IM#2");
		im2.setItemId("IM#2");
		im2.setStandardUom(uomMaster);
		im2.setUpdated(new Timestamp(System.currentTimeMillis()));
		ItemMaster.staticGetDao().store(im2);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		cache.get("IM#MISS");
		Assert.assertEquals(1, cache.size());
		cache.get("IM#2");
		Assert.assertEquals(2, cache.size());
		this.getTenantPersistenceService().commitTransaction();
	}
}
