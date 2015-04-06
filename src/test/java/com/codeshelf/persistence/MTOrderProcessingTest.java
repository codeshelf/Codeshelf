package com.codeshelf.persistence;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.event.EventProducer;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.validation.BatchResult;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class MTOrderProcessingTest extends ServerTest {
	private static final Logger	LOGGER = LoggerFactory.getLogger(MTOrderProcessingTest.class);
	
	UserContext system;
	
	// tenants
	Tenant tenant1;
	Tenant tenant2;
	Tenant tenant3;
	Tenant tenant4;
	Tenant tenant5;
	
	// facilities
	Facility f1;
	Facility f2;
	Facility f3;
	Facility f4;
	Facility f5;
	
	ExecutorService executor;

	@Override
	public void doBefore() {
		super.doBefore();
		CodeshelfSecurityManager.removeContextIfPresent();
		system = CodeshelfSecurityManager.getUserContextSYSTEM();
		executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().build());
	}
	
	private void createDefaultTenantsFacilities() {
		// create five tenants
		tenant1 = this.tenantManagerService.createTenant("t1","t1schema");
		Assert.assertNotNull(tenant1);
		tenant2 = this.tenantManagerService.createTenant("t2","t2schema");
		Assert.assertNotNull(tenant2);
		tenant3 = this.tenantManagerService.createTenant("t3","t3schema");		
		Assert.assertNotNull(tenant3);
		tenant4 = this.tenantManagerService.createTenant("t4","t4schema");		
		Assert.assertNotNull(tenant4);
		tenant5 = this.tenantManagerService.createTenant("t5","t5schema");		
		Assert.assertNotNull(tenant5);
		
		// create five facilities
		CodeshelfSecurityManager.setContext(system, tenant1);
		tenantPersistenceService.beginTransaction();
		this.f1 = Facility.createFacility("f1","", Point.getZeroPoint());
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();

		CodeshelfSecurityManager.setContext(system, tenant2);
		tenantPersistenceService.beginTransaction();
		this.f2 = Facility.createFacility("f2","", Point.getZeroPoint());
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();

		CodeshelfSecurityManager.setContext(system, tenant3);
		tenantPersistenceService.beginTransaction();
		this.f3 = Facility.createFacility("f3","", Point.getZeroPoint());
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();

		CodeshelfSecurityManager.setContext(system, tenant4);
		tenantPersistenceService.beginTransaction();
		this.f4 = Facility.createFacility("f4","", Point.getZeroPoint());
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();

		CodeshelfSecurityManager.setContext(system, tenant5);
		tenantPersistenceService.beginTransaction();
		this.f5 = Facility.createFacility("f5","", Point.getZeroPoint());
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();
	}

	@Override
	public void doAfter() {
		super.doAfter();
		executor.shutdownNow();
	}
	
	@Test
	public void simultaneousTenantImports() throws Exception { 
		// failed asserts in the accessor threads will cause this to throw execution exception		
		createDefaultTenantsFacilities();
		Future<Boolean> result1 = executor.submit(new TenantAccessor(tenant1,f1.getPersistentId()));
		Future<Boolean> result2 = executor.submit(new TenantAccessor(tenant2,f2.getPersistentId()));
		Future<Boolean> result3 = executor.submit(new TenantAccessor(tenant3,f3.getPersistentId()));
		Future<Boolean> result4 = executor.submit(new TenantAccessor(tenant4,f4.getPersistentId()));
		Future<Boolean> result5 = executor.submit(new TenantAccessor(tenant5,f5.getPersistentId()));
		Assert.assertTrue(result1.get());
		Assert.assertTrue(result2.get());
		Assert.assertTrue(result3.get());
		Assert.assertTrue(result4.get());
		Assert.assertTrue(result5.get());
		executor.shutdown();
	}
	
	class TenantAccessor implements Callable<Boolean> {
		Tenant tenant;
		ICsvOrderImporter importer = new OutboundOrderPrefetchCsvImporter(new EventProducer());
		UUID facilityId;
		Random rand = new Random();
		
		TenantAccessor(Tenant t,UUID facilityId) {
			this.tenant = t;
			this.facilityId = facilityId;
		}
		
		@Override
		public Boolean call() throws Exception {
			LOGGER.info("accessing tenant {}",tenant.getName());
			
			try {
				CodeshelfSecurityManager.setContext(system, tenant);
				
				// submit order file
				tenantPersistenceService.beginTransaction();
				Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
				Assert.assertNotNull(facility);				
				String orderId = Integer.toString(rand.nextInt(10000));
				int numLines = rand.nextInt(100)+100;
				int totalQuant = 0;
				String orderString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId";
				for (int i=0;i<numLines;i++) {
					int quant = rand.nextInt(10)+1;
					totalQuant += quant;
					orderString += "\r\n"+orderId+","+orderId+","+i+",12/03/14 12:00,12/31/14 12:00,Item#"+i+",,"+quant+",a,"+tenant.getId();
				}				
				importCsvString(facility, orderString);
				tenantPersistenceService.commitTransaction();

				// verify order data
				tenantPersistenceService.beginTransaction();
				facility = Facility.staticGetDao().reload(facility);
				List<OrderHeader> orders = facility.getOrderHeaders();
				Assert.assertNotNull(orders);
				Assert.assertEquals(1, orders.size());
				OrderHeader order = orders.get(0);
				Assert.assertEquals(orderId, order.getOrderId());
				Assert.assertNotNull(order.getOrderGroup());
				Assert.assertEquals(Integer.toString(tenant.getId()), order.getOrderGroup().getOrderGroupId());
				List<OrderDetail> details = order.getOrderDetails();
				Assert.assertNotNull(details);
				Assert.assertEquals(numLines, details.size());
				int total = 0;
				for (OrderDetail item : details) {
					total += item.getQuantity();
				}
				Assert.assertEquals(totalQuant, total);
				tenantPersistenceService.commitTransaction();			
			}
			catch(Exception e) {
				LOGGER.error("Order import test failed",e);
				throw e;
			}
			finally {
				CodeshelfSecurityManager.removeContext();
			}
			return true;
		}
		
		private BatchResult<Object> importCsvString(Facility facility, String csvString) throws IOException {
			Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
			return importCsvString(facility, csvString, ediProcessTime);
		}
		
		private BatchResult<Object> importCsvString(Facility facility, String csvString, Timestamp ediProcessTime) throws IOException {
			BatchResult<Object> results = importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
			return results;
		}
	}	
}
