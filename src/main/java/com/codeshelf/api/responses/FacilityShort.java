package com.codeshelf.api.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

import com.codeshelf.model.domain.Facility;

public class FacilityShort {
	@Getter
	private String domainId;
	
	@Getter
	private UUID persistentId;
	
	@Getter
	private String description;
	
	
	public FacilityShort(String domainId, UUID persistentId, String description) {
		this.domainId = domainId;
		this.persistentId = persistentId;
		this.description = description;
	}
	
	public static List<FacilityShort> generateList(List<Facility> facilities){
		List<FacilityShort> result = new ArrayList<FacilityShort>();
		for (Facility facility : facilities) {
			result.add(new FacilityShort(facility.getDomainId(), facility.getPersistentId(), facility.getDescription()));
		}
		return result;
	}
}
