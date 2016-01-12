package com.codeshelf.api.responses;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.joda.time.Duration;

import lombok.Getter;

import com.codeshelf.model.domain.Facility;

public class FacilityShort {
	@Getter
	private String domainId;
	
	@Getter
	private UUID persistentId;
	
	@Getter
	private String description;
	
	@Getter
	private long utcOffset;
	
	@Getter
	private String timeZoneDisplay;

	public FacilityShort(Facility facility) {
		this(facility.getDomainId(), facility.getPersistentId(), facility.getDescription(), facility.getTimeZone());
	}
	
	public FacilityShort(String domainId, UUID persistentId, String description, TimeZone timeZone) {
		this.domainId = domainId;
		this.persistentId = persistentId;
		this.description = description;
		this.utcOffset = new Duration(timeZone.getRawOffset()).getStandardMinutes();
		this.timeZoneDisplay = timeZone.getDisplayName(timeZone.inDaylightTime(new Date()), TimeZone.SHORT);
	}
	
	public static List<FacilityShort> generateList(List<Facility> facilities){
		List<FacilityShort> result = new ArrayList<FacilityShort>();
		for (Facility facility : facilities) {
			result.add(new FacilityShort(facility.getDomainId(), facility.getPersistentId(), facility.getDescription(), facility.getTimeZone()));
		}
		return result;
	}
}
