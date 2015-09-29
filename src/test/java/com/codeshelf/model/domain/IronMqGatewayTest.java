package com.codeshelf.model.domain;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import io.iron.ironmq.Client;
import io.iron.ironmq.HTTPException;
import io.iron.ironmq.Queue;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.testframework.MockDaoTest;

public class IronMqGatewayTest extends MockDaoTest {
	@Before
	public void doBefore() {
		super.doBefore();
		@SuppressWarnings("unchecked")
		ITypedDao<IronMqGateway> mock = mock(ITypedDao.class);
		super.useCustomDao(IronMqGateway.class, mock);
	}
	
	@Test
	public void whenEmptyCredentialsThrowException() throws IOException {
		Queue queue = mock(Queue.class);
		IronMqGateway service = new IronMqGateway(createClientProvider("", "", queue));
		service.storeCredentials("", "", "true");
		service.transportWiFinished("AMESSAGE", "AMESSAGE", "AMESSAGE");
		Mockito.verifyZeroInteractions(queue);
	}
	
	//@Test
	public void whenQueueDoesNotExistThrowsIOException() {
		
	}
	
	@Test
	public void withWrongCredentialsThrowsIOException()  throws IOException {
		String projectId = "TESTPROJECT";
		String token = "TESTTOKEN";
		
		Queue queueForBadCredentials = mock(Queue.class);
		doThrow(new HTTPException(404, "Not found")).when(queueForBadCredentials).push(any(String.class));
		
		
		IronMqGateway service = new IronMqGateway(createClientProvider(projectId, token, queueForBadCredentials));
		service.storeCredentials(projectId, token, "true");
		try {
			service.transportWiFinished("AMESSAGE", "AMESSAGE", "AMESSAGE");
			Assert.fail("should have thrown an IOException");
		}
		catch(IOException e) {
			
		}
		
	}

	@Test
	public void whenPushSuccessfulReturn() throws IOException {
		String projectId = "TESTPROJECT";
		String token = "TESTTOKEN";
		
		Queue queue = mock(Queue.class);
		doReturn("MESSAGEID").when(queue).push(any(String.class));
		
		IronMqGateway.ClientProvider provider = createClientProvider(projectId, token, queue);
		
		
		IronMqGateway service = new IronMqGateway(provider);
		service.storeCredentials(projectId, token, "true");
		service.transportWiFinished("AMESSAGE", "AMESSAGE", "AMESSAGE");
	}

	
	@Test
	public void whenPushThrowsHTTPExceptionSubjectThrowsIOException() throws IOException {
		String projectId = "TESTPROJECT";
		String token = "TESTTOKEN";

		Queue queue = mock(Queue.class);
		doThrow(new HTTPException(401,"Unauthorized")).when(queue).push(any(String.class));
		IronMqGateway.ClientProvider provider = createClientProvider(projectId, token, queue);

		IronMqGateway service = new IronMqGateway(provider);
		service.storeCredentials(projectId, token, "true");
		try {
			service.transportWiFinished("AMESSAGE", "AMESSAGE", "AMESSAGE");
			Assert.fail("should have thrown an IOException");
		}
		catch(IOException e) {
			
		}
		
	}
	
	@Test
	public void whenPushThrowsIOExceptionSubjectThrowsIOException() throws IOException {
		String projectId = "TESTPROJECT";
		String token = "TESTTOKEN";

		Queue queue = mock(Queue.class);
		doThrow(new IOException()).when(queue).push(any(String.class));
		IronMqGateway.ClientProvider provider = createClientProvider(projectId, token, queue);

		IronMqGateway service = new IronMqGateway(provider);
		service.storeCredentials(projectId, token, "true");
		try {
			service.transportWiFinished("AMESSAGE", "AMESSAGE", "AMESSAGE");

			Assert.fail("should have thrown an IOException");
		}
		catch(IOException e) {
			
		}
		
	}
	
	private IronMqGateway.ClientProvider createClientProvider(String projectId, String token, Queue queue) {
		Client client = mock(Client.class);
		doReturn(queue).when(client).queue(any(String.class));
		
		IronMqGateway.ClientProvider provider = mock(IronMqGateway.ClientProvider.class);
		doReturn(client).when(provider).get(eq(projectId), eq(token));
		return provider;
	}
	
}
