package com.gadgetworks.codeshelf.device;

import java.util.UUID;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.CommandABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.LoginCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.PingCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkStatusRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.PingRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.CompleteWorkInstructionResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ComputeWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.GetWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkAttachResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkStatusResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class SiteControllerMessageProcessor extends MessageProcessor {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(SiteControllerMessageProcessor.class);
	
	private JettyWebSocketClient	client;

	private CsDeviceManager	deviceManager;

	public SiteControllerMessageProcessor(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}
	
	@Override
	public void handleResponse(Session session, ResponseABC response) {
		LOGGER.info("Response received:"+response);
		if (response.getStatus()!=ResponseStatus.Success) {
			LOGGER.warn("Request #"+response.getRequestId()+" failed: "+response.getStatusMessage());
			return;
		}
		//////////////////////////////////////////
		// Handler for Network Attach Response
		if (response instanceof NetworkAttachResponse) {
			NetworkAttachResponse naResponse = (NetworkAttachResponse) response;
			if (response.getStatus()==ResponseStatus.Success) {
				LOGGER.info("Attached to network");
				UUID networkId = naResponse.getNetworkId();
				// request current network status
				NetworkStatusRequest req = new NetworkStatusRequest();
				req.setNetworkId(networkId);
				this.client.sendRequest(req);		
			}
		}
		//////////////////////////////////////////
		// Handler for Network Update
		else if (response instanceof NetworkStatusResponse) {
			NetworkStatusResponse update = (NetworkStatusResponse) response;
			if (response.getStatus()==ResponseStatus.Success) {
				this.deviceManager.updateNetwork(update.getChes(),update.getLedControllers());
			}
		}
		//////////////////////////////////////////
		// Handler for Compute Work
		else if (response instanceof ComputeWorkResponse) {
			ComputeWorkResponse computeWorkResponse = (ComputeWorkResponse) response;
			if (response.getStatus()==ResponseStatus.Success) {
				this.deviceManager.processComputeWorkResponse(computeWorkResponse.getNetworkGuid(),computeWorkResponse.getWorkInstructionCount());
			}
		}
		//////////////////////////////////////////
		// Handler for Get Work
		else if (response instanceof GetWorkResponse) {
			GetWorkResponse workResponse = (GetWorkResponse) response;
			if (response.getStatus()==ResponseStatus.Success) {
				this.deviceManager.processGetWorkResponse(workResponse.getNetworkGuid(),workResponse.getWorkInstructions());
			}
		}		
		//////////////////////////////////////////
		// Handler for WI Completion
		else if (response instanceof CompleteWorkInstructionResponse) {
			CompleteWorkInstructionResponse workResponse = (CompleteWorkInstructionResponse) response;
			if (response.getStatus()==ResponseStatus.Success) {
				this.deviceManager.processWorkInstructionCompletedResponse(workResponse.getWorkInstructionId());
			}
		}			
		else {
			LOGGER.warn("Failed to handle response "+response);
		}
	}
	
	@Override
	public ResponseABC handleRequest(Session session, RequestABC request) {
		LOGGER.info("Request received for processing: "+request);
		CommandABC command = null;
		ResponseABC response = null;
		
		if (request instanceof PingRequest) {
			command = new PingCommand((PingRequest) request);
		}		
		// check if matching command was found
		if (command==null) {
			LOGGER.warn("Unable to find matching command for request "+request+". Ignoring request.");
			return null;
		}		
		// execute command and generate response to be sent to client
		response = command.exec();
		if (response!=null) {
			// automatically tie response to request
			response.setRequestId(request.getMessageId());
		}
		else {
			LOGGER.warn("No response generated for request "+request);
		}
		return response;
	}
	
	public void setWebClient(JettyWebSocketClient client) {
		this.client = client;
	}

}
