package com.codeshelf.metrics;


import java.util.List;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.ExtensionPointService;

public class DataQuantityHealthCheck extends CodeshelfHealthCheck {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(DataQuantityHealthCheck.class);

	int							failedFacilityCount	= 0;
	int							totalFacilityCount	= 0;

	public DataQuantityHealthCheck() {
		super("DataQuantity");
	}

	@Override
	protected Result check() throws Exception {

		int totalTenantCount = 0;
		failedFacilityCount = 0;
		totalFacilityCount = 0;

		List<Tenant> tenants = TenantManagerService.getInstance().getTenants();
		totalTenantCount = tenants.size();
		for (Tenant tenant : tenants) {
			try {
				CodeshelfSecurityManager.setContext(CodeshelfSecurityManager.getUserContextSYSTEM(), tenant);
				checkFacilitiesForTenant(tenant);
			} finally {
				CodeshelfSecurityManager.removeContext();
			}
		}

		if (failedFacilityCount == 0) {
			return Result.healthy("Data quantity for %d facilities in %d tenants ok.", totalFacilityCount, totalTenantCount);
		} else {
			return Result.unhealthy("Data quantity for %d of %d facilities in %d tenants outside of limits. See log.",
				failedFacilityCount,
				totalFacilityCount,
				totalTenantCount);
		}
	}

	private void checkFacilitiesForTenant(Tenant tenant) {
		int totalFacilitiesThisTenant = 0;
		int badFacilitiesThisTenant = 0;

		try {
			TenantPersistenceService.getInstance().beginTransaction();
			List<Facility> allFacilities = Facility.staticGetDao().getAll();
			totalFacilitiesThisTenant = allFacilities.size();
			for (Facility facility : allFacilities) {
				if (!checkFacilityOk(facility)) {
					badFacilitiesThisTenant++;
				}
			}
		} finally {
			TenantPersistenceService.getInstance().rollbackTransaction(); // we did not change anything
		}

		totalFacilityCount += totalFacilitiesThisTenant;
		failedFacilityCount += badFacilitiesThisTenant;

	}

	private boolean checkFacilityOk(Facility inFacility) {
		boolean foundFacilityProblem = false;
		
		int			MAX_ORDERDETAIL		= 100000;
		int			MAX_WORKINSTRUCTION	= 100000;
		int			MAX_ORDER			= 40000;
		int			MAX_CONTAINERUSE	= 40000;


		try {
			ExtensionPointService theService = ExtensionPointService.createInstance(inFacility);
			DataQuantityHealthCheckParameters params = theService.getDataQuantityHealthCheckParameters();
			MAX_ORDERDETAIL = params.getMaxOrderDetailValue();
			MAX_WORKINSTRUCTION = params.getMaxWorkInstructionValue();
			MAX_ORDER = params.getMaxOrderValue();
			MAX_CONTAINERUSE = params.getMaxContainerUseValue();
		

		} catch (javax.script.ScriptException e) {
			LOGGER.error("checkFacilityOk", e);
		}

		UUID facilityUUID = inFacility.getPersistentId();

		// we might want to ask the facility for its personal limits.

		// Work Instructions
		Criteria totalWisCrit = WorkInstruction.staticGetDao().createCriteria();
		totalWisCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		int totalWiCount = WorkInstruction.staticGetDao().countByCriteriaQuery(totalWisCrit);
		if (totalWiCount > MAX_WORKINSTRUCTION) {
			foundFacilityProblem = true;
			reportQuantityProblem(inFacility, totalWiCount, MAX_WORKINSTRUCTION, "WorkInstruction");
		}

		// Orders
		Criteria totalOrdersCrit = OrderHeader.staticGetDao().createCriteria();
		totalOrdersCrit.add(Restrictions.eq("parent.persistentId", facilityUUID));
		int totalOrderCount = OrderHeader.staticGetDao().countByCriteriaQuery(totalOrdersCrit);
		if (totalOrderCount > MAX_ORDER) {
			foundFacilityProblem = true;
			reportQuantityProblem(inFacility, totalOrderCount, MAX_ORDER, "OrderHeader");
		}

		// Order Details
		Criteria totalDetailsCrit = OrderDetail.staticGetDao().createCriteria();
		totalDetailsCrit.createAlias("parent", "p");
		totalDetailsCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		int totalDetailCount = OrderDetail.staticGetDao().countByCriteriaQuery(totalDetailsCrit);
		if (totalDetailCount > MAX_ORDERDETAIL) {
			foundFacilityProblem = true;
			reportQuantityProblem(inFacility, totalDetailCount, MAX_ORDERDETAIL, "OrderDetail");
		}

		// ContainerUse
		Criteria totalUsesCrit = ContainerUse.staticGetDao().createCriteria();
		totalUsesCrit.createAlias("parent", "p");
		totalUsesCrit.add(Restrictions.eq("p.parent.persistentId", facilityUUID));
		int totalUseCount = ContainerUse.staticGetDao().countByCriteriaQuery(totalUsesCrit);
		if (totalUseCount > MAX_CONTAINERUSE) {
			foundFacilityProblem = true;
			reportQuantityProblem(inFacility, totalUseCount, MAX_CONTAINERUSE, "ContainerUse");
		}

		return !foundFacilityProblem;
	}

	private void reportQuantityProblem(Facility facility, int totalObjectCount, int maxObject, String objectKind) {
		LOGGER.warn("HealthCheck failure for {}. {} count: {} > recommended limit: {}",
			facility,
			objectKind,
			totalObjectCount,
			maxObject);

	}

}
