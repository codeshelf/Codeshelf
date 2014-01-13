package com.gadgetworks.codeshelf.ws.command.req;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.util.UUID;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.WsRespCmdEnum;

@RunWith(MockitoJUnitRunner.class)
public class ObjectUpdateWsReqCmdTest {
	
	@Mock
	private ITypedDao<Facility> mockTypedDao;
	
	@Mock
	private IDaoProvider mockDaoProvider;

	@Mock
	private Facility mockFacility;

	
	@Test
	public void shouldReturnErrorResponseWhenClassDoesntExist() throws JsonProcessingException, IOException {
		ObjectNode mockJsonNode = createReqCmdJsonNode();
		mockJsonNode.put(IWsReqCmd.CLASSNAME, "com.gadgetworks.codeshelf.model.domain.NOTFOUND");
		
		ObjectUpdateWsReqCmd subject = new ObjectUpdateWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();
		Assert.assertEquals(WsRespCmdEnum.INVALID, respCmd.getCommandEnum());
	}

	@Test
	public void shouldReturnErrorResponseWhenInstanceNotFound() throws JsonProcessingException, IOException {
		when(mockTypedDao.findByPersistentId(any(UUID.class))).thenReturn(null);

		when(mockDaoProvider.getDaoInstance(Facility.class)).thenReturn(mockTypedDao);

		
		ObjectNode mockJsonNode = createReqCmdJsonNode();
		
		ObjectUpdateWsReqCmd subject = new ObjectUpdateWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();
		Assert.assertEquals(WsRespCmdEnum.INVALID, respCmd.getCommandEnum());
	}

	@Test
	public void shouldReturnErrorResponseWhenDaoStoreException() throws JsonProcessingException, IOException {
		doThrow(new DaoException("test")).when(mockTypedDao).store(any(Facility.class));
		
		when(mockDaoProvider.getDaoInstance(Facility.class)).thenReturn(mockTypedDao);
		when(mockTypedDao.findByPersistentId(any(UUID.class))).thenReturn(mockFacility);
		
		
		ObjectNode mockJsonNode = createReqCmdJsonNode();
		
		ObjectUpdateWsReqCmd subject = new ObjectUpdateWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();
		Assert.assertEquals(WsRespCmdEnum.INVALID, respCmd.getCommandEnum());
	}

	
	private ObjectNode createReqCmdJsonNode() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode = mapper.createObjectNode();
		objectNode.put(IWsReqCmd.CLASSNAME, "com.gadgetworks.codeshelf.model.domain.Facility");
		objectNode.put(IWsReqCmd.PERSISTENT_ID, UUID.randomUUID().toString());
		objectNode.put(IWsReqCmd.PROPERTIES, mapper.createArrayNode());
		return objectNode;
	}
	
	

}
