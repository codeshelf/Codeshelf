package com.codeshelf.metrics;

import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.ExtensionPointEngine;

public class DataQuantityHealthCheck extends HealthCheckRefreshJob{
	
	private static final Logger	LOGGER				= LoggerFactory.getLogger(DataQuantityHealthCheck.class);

	@Override
	public void check(Facility facility) throws Exception {
		boolean success = checkFacilityOk(facility);
		if (success) {
			saveResults(facility, true, "Data quantity ok");
		} else {
			saveResults(facility, false, "Data quantity outside of limits. See log");
		}
	}

	private boolean checkFacilityOk(Facility inFacility) {
		boolean foundFacilityProblem = false;
		
		int			MAX_ORDERDETAIL		= 100000;
		int			MAX_WORKINSTRUCTION	= 100000;
		int			MAX_ORDER			= 40000;
		int			MAX_CONTAINERUSE	= 40000;


		try {
			ExtensionPointEngine theService = ExtensionPointEngine.getInstance(inFacility);
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
