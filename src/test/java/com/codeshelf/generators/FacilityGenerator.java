package com.codeshelf.generators;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;

public class FacilityGenerator {
	public FacilityGenerator() {
	}
	
	public Facility generateValid() {
		Facility facility = Facility.createFacility("F1", "", Point.getZeroPoint());
		CodeshelfNetwork network = facility.getNetworks().get(0);
		network.createChe("CHE1", new NetGuid("0xdeadbeef"), ColorEnum.MAGENTA);
		return facility;
	}
}
