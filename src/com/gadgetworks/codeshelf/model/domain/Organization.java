package com.gadgetworks.codeshelf.model.domain;

import java.util.List;
import java.util.UUID;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;

@Deprecated
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@JsonIgnoreProperties({"className"})
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Organization {
	@Getter
	@JsonProperty
	UUID	persistentId	= UUID.randomUUID();

	public boolean createFacilityUi(String domainId, String description, Double x, Double y) {
		Point point = new Point(PositionTypeEnum.GPS, x, y, null);
		return (null != Facility.createFacility(TenantManagerService.getInstance().getDefaultTenant(), domainId, description, point));
	}

	public Facility getFacility(final String inFacilityDomainId) {
		return Facility.DAO.findByDomainId(null, inFacilityDomainId);
	}

	public List<Facility> getFacilities() {
		return Facility.DAO.getAll();
	}
}
