package com.codeshelf.generators;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;

public class FacilityGenerator {
	public FacilityGenerator() {
	}
	
	public Facility generateValid() {
		Facility facility = Facility.createFacility("F1", "", Point.getZeroPoint());
		return facility;
	}
}
