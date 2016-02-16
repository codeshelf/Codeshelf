package com.codeshelf.model;

import lombok.Getter;
import lombok.Setter;

public class ReplenishItem {

	@Getter @Setter	
	String itemId;		
	
	@Getter @Setter
	String gtin;
	
	@Getter @Setter
	String uom;
	
	@Getter @Setter
	String location;

}
