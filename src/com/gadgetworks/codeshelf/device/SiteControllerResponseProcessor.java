package com.gadgetworks.codeshelf.device;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.client.ResponseProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.NetworkStatusRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.CompleteWorkInstructionResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ComputeWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.GetWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkAttachResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.NetworkStatusResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;

public class SiteControllerResponseProcessor extends ResponseProcessor {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(SiteControllerResponseProcessor.class);
	
	private JettyWebSocketClient	client;

	private CsDeviceManager	deviceManager;

	public SiteControllerResponseProcessor(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}
	
	@Override
	public void handleResponse(ResponseABC response) {
		LOGGER.info("Response received:"+response);
		if (response.getStatus()!=ResponseStatus.Success) {
			LOGGER.warn("Request #"+response.getRequestID()+" failed: "+response.getStatusMessage());
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

	public void setWebClient(JettyWebSocketClient client) {
		this.client = client;
	}
}
