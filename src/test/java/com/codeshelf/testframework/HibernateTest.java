package com.codeshelf.testframework;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HibernateTest extends MockDaoTest {
	@SuppressWarnings("unused")
	private final static Logger LOGGER = LogManager.getLogger(HibernateTest.class);

	@Override
	Type getFrameworkType() {
		return Type.HIBERNATE;
	}
	
	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return false;
	}


}
