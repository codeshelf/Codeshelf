package com.gadgetworks.codeshelf.model.domain;

import io.iron.ironmq.Client;
import io.iron.ironmq.HTTPException;
import io.iron.ironmq.Queue;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import com.gadgetworks.codeshelf.edi.WorkInstructionCSVExporter;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC;
import com.gadgetworks.codeshelf.model.domain.IronMqService;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ImmutableList;

public class IronMqServiceTest {

	@Before
	public void doBefore() {
		IronMqService.DAO = mock(ITypedDao.class);
	}
	
	@Test
	public void whenEmptyCredentialsThrowException() throws IOException {
		IronMqService service = new IronMqService();
		service.storeCredentials("", "");
		try {
			service.sendWorkInstructionsToHost(ImmutableList.of(mock(WorkInstruction.class)));
			Assert.fail("should have thrown an IllegalStateException");
		}
		catch(IllegalStateException e) {
			
		}
		
	}
	
	@Test
	public void whenQueueDoesNotExistThrowsIOException() {
		
	}
	
	@Test
	public void withWrongCredentialsThrowsIOException()  throws IOException {
		String projectId = "TESTPROJECT";
		String token = "TESTTOKEN";
		
		Queue queueForBadCredentials = mock(Queue.class);
		doThrow(new HTTPException(404, "Not found")).when(queueForBadCredentials).push(any(String.class));
		
		
		WorkInstructionCSVExporter exporter = spy(new WorkInstructionCSVExporter());
		doReturn("AMESSAGE").when(exporter).exportWorkInstructions(any(List.class));

		IronMqService service = new IronMqService(exporter, createClientProvider(projectId, token, queueForBadCredentials));
		service.storeCredentials(projectId, token);
		try {
			service.sendWorkInstructionsToHost(ImmutableList.of(mock(WorkInstruction.class)));
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
		
		IronMqService.ClientProvider provider = createClientProvider(projectId, token, queue);
		
		
		WorkInstructionCSVExporter exporter = spy(new WorkInstructionCSVExporter());
		doReturn("AMESSAGE").when(exporter).exportWorkInstructions(any(List.class));
		IronMqService service = new IronMqService(exporter, provider);
		service.storeCredentials(projectId, token);
		service.sendWorkInstructionsToHost(ImmutableList.of(mock(WorkInstruction.class)));
	}

	
	@Test
	public void whenPushThrowsHTTPExceptionSubjectThrowsIOException() throws IOException {
		String projectId = "TESTPROJECT";
		String token = "TESTTOKEN";

		Queue queue = mock(Queue.class);
		doThrow(new HTTPException(401,"Unauthorized")).when(queue).push(any(String.class));
		IronMqService.ClientProvider provider = createClientProvider(projectId, token, queue);

		WorkInstructionCSVExporter exporter = spy(new WorkInstructionCSVExporter());
		doReturn("AMESSAGE").when(exporter).exportWorkInstructions(any(List.class));
		IronMqService service = new IronMqService(exporter, provider);
		service.storeCredentials(projectId, token);
		try {
			service.sendWorkInstructionsToHost(ImmutableList.of(mock(WorkInstruction.class)));
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
		IronMqService.ClientProvider provider = createClientProvider(projectId, token, queue);

		WorkInstructionCSVExporter exporter = spy(new WorkInstructionCSVExporter());
		doReturn("AMESSAGE").when(exporter).exportWorkInstructions(any(List.class));
		IronMqService service = new IronMqService(exporter, provider);
		service.storeCredentials(projectId, token);
		try {
			service.sendWorkInstructionsToHost(ImmutableList.of(mock(WorkInstruction.class)));
			Assert.fail("should have thrown an IOException");
		}
		catch(IOException e) {
			
		}
		
	}
	
	private IronMqService.ClientProvider createClientProvider(String projectId, String token, Queue queue) {
		Client client = mock(Client.class);
		doReturn(queue).when(client).queue(any(String.class));
		
		IronMqService.ClientProvider provider = mock(IronMqService.ClientProvider.class);
		doReturn(client).when(provider).get(eq(projectId), eq(token));
		return provider;
	}
	
}
