/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.gadgetworks.codeshelf.model.dao.IDao;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.ObjectUpdateCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ObjectUpdateRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.collect.ImmutableMap;

/**
 * @author ranstrom
 *
 */
public class CheTest extends DomainTestABC {

	@SuppressWarnings("unchecked")
	@Test
	public final void setCheColor() {
		Che testChe = new Che();
		testChe.setPersistentId(UUID.randomUUID());
		Assert.assertNotEquals(ColorEnum.GREEN, testChe.getColor());
		ObjectUpdateRequest testRequest = new ObjectUpdateRequest(testChe.getClass().getSimpleName(),
			testChe.getPersistentId(),
			ImmutableMap.of("color", (Object) ColorEnum.GREEN.name()));
		
		IDaoProvider mockProvider = mock(IDaoProvider.class);
		ITypedDao mockDao = mock(ITypedDao.class);
		
		when(mockProvider.getDaoInstance(Mockito.any(Class.class))).thenReturn(mockDao);
		when(mockDao.findByPersistentId(Mockito.eq(testChe.getPersistentId()))).thenReturn(testChe);
		ObjectUpdateCommand changeCheColor = new ObjectUpdateCommand(mockProvider, mock(CsSession.class), testRequest);
		ResponseABC response = changeCheColor.exec();
		Assert.assertTrue(response.isSuccess());
		ArgumentCaptor<Che> cheCapture = ArgumentCaptor.forClass(Che.class);
		verify(mockDao, times(1)).store(cheCapture.capture());
	
		Assert.assertEquals(ColorEnum.GREEN, cheCapture.getValue().getColor());
	}
	
	@Test
	public final void trivialCheTest() {
		Facility facility = createFacility("CTEST1.O1");
		Assert.assertNotNull(facility);
		
		Che newChe = new Che(); // not right
		// This throws horribly. But we catch it.
		try {
			// kind of stupid. Just see if this makes the compiler not strip out the method.
			newChe.changeControllerId("0x000089");
		}
		catch (Exception e) {
			
		}
		
		
	}


}
