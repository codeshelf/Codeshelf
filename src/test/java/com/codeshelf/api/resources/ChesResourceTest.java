package com.codeshelf.api.resources;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.testframework.HibernateTest;

public class ChesResourceTest extends HibernateTest {

	/**
	 * Add "encode" function equivalent like POSTGRES has 
	 */
	public static String encode(byte[] guid, String type) {
		if (type.equals("hex")) {
			return new NetGuid(guid).getHexStringNoPrefix();
		}
		throw new IllegalArgumentException("unexpected encode type: " + type);
	}
	
	
	@Override
	public void doBefore() {
		super.doBefore();
		beginTransaction();
		TenantPersistenceService.getInstance().getSession().createSQLQuery("CREATE ALIAS IF NOT EXISTS encode FOR \"com.codeshelf.api.resources.ChesResourceTest.encode\"").executeUpdate();
		commitTransaction();
	}

	@Test
	public void testFindChePartialId() {
		ChesResource subject = new ChesResource();
		beginTransaction();
		Facility facility = getFacility();
		subject.setFacility(facility);
		
		Response response = subject.getAllChes("*C*", 2);
		@SuppressWarnings("unchecked")
		ResultDisplay<Che> entity = (ResultDisplay<Che>) response.getEntity();
		List<Che> ches= new ArrayList<>(entity.getResults());
		assertEquals(2, ches.size());
		assertNotEquals(ches.get(0), ches.get(1));
		commitTransaction();

	}

	@Test
	public void testFindChePartialGuid() {
		
		
		ChesResource subject = new ChesResource();
		beginTransaction();
		Facility facility = getFacility();
		subject.setFacility(facility);
		
		Response response = subject.getAllChes("*92*", 2);
		@SuppressWarnings("unchecked")
		ResultDisplay<Che> entity = (ResultDisplay<Che>) response.getEntity();
		List<Che> ches= new ArrayList<>(entity.getResults());
		assertEquals(1, ches.size());
		assertTrue(ches.get(0).getDeviceGuidStr().contains("92"));
		commitTransaction();

	}

	
	@Test
	public void testFindCheWithoutGlob() {
		ChesResource subject = new ChesResource();
		beginTransaction();
		Facility facility = getFacility();
		subject.setFacility(facility);
		
		Response response = subject.getAllChes("C", 2);
		@SuppressWarnings("unchecked")
		ResultDisplay<Che> entity = (ResultDisplay<Che>) response.getEntity();
		List<Che> ches= new ArrayList<>(entity.getResults());
		assertEquals(0, ches.size());
		commitTransaction();

	}

	
}
