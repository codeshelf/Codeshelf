package com.codeshelf.persistence;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.testframework.HibernateTest;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class MultitenancyTest extends HibernateTest {
	private static final Logger					LOGGER					= LoggerFactory.getLogger(MultitenancyTest.class);
	
	UserContext system;
	Tenant tenant1;
	Tenant tenant2;
	Tenant tenant3;
	ExecutorService executor;
	
	@Override
	public void doBefore() {
		super.doBefore();
		CodeshelfSecurityManager.removeContextIfPresent();
		system = CodeshelfSecurityManager.getUserContextSYSTEM();
		executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().build());
	}
	
	private void createDefaultTenantsFacilities() {
		tenant1 = getDefaultTenant();
		tenant2 = this.tenantManagerService.createTenant("t2","t2schema");
		Assert.assertNotNull(tenant2);
		tenant3 = this.tenantManagerService.createTenant("t3","t3schema");		
		Assert.assertNotNull(tenant3);

		CodeshelfSecurityManager.setContext(system, tenant1);
		tenantPersistenceService.beginTransaction();
		Facility f1 = this.getFacility();
		UomMaster.staticGetDao().store(f1.createUomMaster("perServer"));
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();

		CodeshelfSecurityManager.setContext(system, tenant2);
		tenantPersistenceService.beginTransaction();
		Facility f2 = Facility.createFacility("f2","", Point.getZeroPoint());
		UomMaster.staticGetDao().store(f2.createUomMaster("perTest"));
		UomMaster.staticGetDao().store(f2.createUomMaster("perClass"));
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();
		
		CodeshelfSecurityManager.setContext(system, tenant3);
		tenantPersistenceService.beginTransaction();
		Facility f3 = Facility.createFacility("f3","", Point.getZeroPoint());
		UomMaster.staticGetDao().store(f3.createUomMaster("perTest"));
		UomMaster.staticGetDao().store(f3.createUomMaster("perClass"));
		UomMaster.staticGetDao().store(f3.createUomMaster("perServer"));
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();
	}

	@Override
	public void doAfter() {
		super.doAfter();
		executor.shutdownNow();
	}
	
	@Test
	public void oneAtATimeTenantAccess() {
		createDefaultTenantsFacilities();
		
		CodeshelfSecurityManager.setContext(system, tenant2);
		tenantPersistenceService.beginTransaction();
		Facility f3 = Facility.staticGetDao().findByDomainId(null, "f3");
		Assert.assertNull(f3);;
		Facility f1 = Facility.staticGetDao().findByDomainId(null, "f1");
		Assert.assertNull(f1);;
		Facility f2 = Facility.staticGetDao().findByDomainId(null, "f2");		
		Assert.assertEquals(2, f2.getUomMasters().size());
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();

		CodeshelfSecurityManager.setContext(system, tenant3);
		tenantPersistenceService.beginTransaction();
		f3 = Facility.staticGetDao().findByDomainId(null, "f3");
		Assert.assertNotNull(f3.getUomMaster("perServer"));
		tenantPersistenceService.commitTransaction();
		CodeshelfSecurityManager.removeContext();
	}
	
	@Test
	public void simultaneousTenantAccess() throws Exception { 
		// failed asserts in the accessor threads will cause this to throw executionexception
		
		createDefaultTenantsFacilities();
		Future<Boolean> result1 = executor.submit(new TenantAccessor(tenant1,true));
		Future<Boolean> result2 = executor.submit(new TenantAccessor(tenant2,true));
		Future<Boolean> result3 = executor.submit(new TenantAccessor(tenant3,true));
		Future<Boolean> result3a = executor.submit(new TenantAccessor(tenant3,false));
		Assert.assertTrue(result1.get());
		Assert.assertTrue(result2.get());
		Assert.assertTrue(result3.get());
		Assert.assertTrue(result3a.get());
		executor.shutdown();
	}
	
	class TenantAccessor implements Callable<Boolean> {
		Tenant tenant;
		boolean changeTenant;
		TenantAccessor(Tenant t,boolean changeTenant) {
			this.tenant = t;
			this.changeTenant = changeTenant;
		}
		@Override
		public Boolean call() throws Exception {
			LOGGER.info("accessing tenant {}",tenant.getName());
			int starting = -1;
			for(int i=0;i<10;i++) {
				CodeshelfSecurityManager.setContext(system, tenant);
				tenantPersistenceService.beginTransaction();
				Facility f = Facility.staticGetDao().getAll().get(0);
				int numUoms = f.getUomMasters().size();
				if(changeTenant) {
					if(starting == -1) starting = numUoms;
					Assert.assertEquals(starting+i,numUoms);
					UomMaster uom = f.createUomMaster("uom"+i);
					UomMaster.staticGetDao().store(uom);
				} else {
					Assert.assertTrue(numUoms > 0);
					LOGGER.info("watching tenant {} have {} uoms",tenant.getName(),numUoms);
				}
				tenantPersistenceService.commitTransaction();
				CodeshelfSecurityManager.removeContext();
			}
			return true;
		}
	}
}
