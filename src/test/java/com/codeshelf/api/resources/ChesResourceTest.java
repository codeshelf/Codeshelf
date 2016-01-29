package com.codeshelf.api.resources;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.codeshelf.api.responses.ResultDisplay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.testframework.HibernateTest;

public class ChesResourceTest extends HibernateTest {

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
