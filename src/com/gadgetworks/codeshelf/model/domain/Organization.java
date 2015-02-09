package com.gadgetworks.codeshelf.model.domain;

import java.util.UUID;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;

import lombok.Getter;

public class Organization {
	@Getter
	UUID persistentId = UUID.randomUUID();
	
	public boolean createFacilityUi(String domainId, String description, Double x, Double y) {
		Point point = new Point(PositionTypeEnum.GPS,x,y,null);
		return (null != Facility.createFacility(TenantManagerService.getInstance().getDefaultTenant(), domainId, description, point));
	}
}
