package com.gadgetworks.codeshelf.api.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

import com.gadgetworks.codeshelf.model.domain.Facility;

public class FacilityShort {
	@Getter
	private String domainId;
	@Getter
	private UUID persistentId;
	
	public FacilityShort(String domainId, UUID persistentId) {
		this.domainId = domainId;
		this.persistentId = persistentId;
	}
	
	public static List<FacilityShort> generateList(List<Facility> facilities){
		List<FacilityShort> result = new ArrayList<FacilityShort>();
		for (Facility facility : facilities) {
			result.add(new FacilityShort(facility.getDomainId(), facility.getPersistentId()));
		}
		return result;
	}
}
