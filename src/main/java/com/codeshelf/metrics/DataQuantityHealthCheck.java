package com.codeshelf.metrics;

import com.codeshelf.model.domain.Facility;

public class DataQuantityHealthCheck extends CodeshelfHealthCheck {
	final static int	MAX_ORDERDETAIL		= 100000;
	final static int	MAX_WORKINSTRUCTION	= 100000;
	final static int	MAX_ORDER			= 40000;
	final static int	MAX_CONTAINERUSE	= 40000;


	public DataQuantityHealthCheck(Facility inFacility) {
		super("DataQuantity");

	}

	@Override
	protected Result check() throws Exception {

		return Result.healthy(OK);
	}

}
