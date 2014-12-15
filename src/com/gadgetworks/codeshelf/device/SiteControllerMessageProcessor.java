package com.gadgetworks.codeshelf.device;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.CommandABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.command.PingCommand;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageProcessor;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.NetworkStatusMessage;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.PingRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.RequestABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.CompleteWorkInstructionResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ComputeWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.GetWorkResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;
import com.gadgetworks.flyweight.command.NetGuid;

public class SiteControllerMessageProcessor extends MessageProcessor {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(SiteControllerMessageProcessor.class);

	private JettyWebSocketClient	client;

	private CsDeviceManager			deviceManager;

	public SiteControllerMessageProcessor(CsDeviceManager deviceManager) {
		this.deviceManager = deviceManager;
	}

	@Override
	public void handleResponse(UserSession session, ResponseABC response) {
		LOGGER.debug("Response received:" + response);
		if (response.getStatus() != ResponseStatus.Success) {
			LOGGER.warn("Request #" + response.getRequestId() + " failed: " + response.getStatusMessage());
		}
		//////////////////////////////////////////
		// Handler for Login Response
		if (response instanceof LoginResponse) {
			LoginResponse loginResponse = (LoginResponse) response;
			boolean attached = false;
			if (loginResponse.getStatus() == ResponseStatus.Success) {
				CodeshelfNetwork network = loginResponse.getNetwork();
				if (network != null) {
					LOGGER.info("Attached to network " + network.getDomainId());
					attached = true;
					this.deviceManager.attached(network);
				} else {
					LOGGER.error("loginResponse has no network");
				}
			}
			if (!attached) {
				LOGGER.warn("Failed to attach network: " + response.getStatusMessage());
				try {
					this.deviceManager.unattached();
					client.disconnect();
				} catch (IOException e) {
					LOGGER.error("Failed to disconnect client", e);
				}
			}
		}
		//////////////////////////////////////////
		// Handler for Compute Work
		else if (response instanceof ComputeWorkResponse) {
			ComputeWorkResponse computeWorkResponse = (ComputeWorkResponse) response;
			if (response.getStatus() == ResponseStatus.Success) {
				this.deviceManager.processComputeWorkResponse(computeWorkResponse.getNetworkGuid(),
					computeWorkResponse.getTotalGoodWorkInstructions(),
					computeWorkResponse.getContainerToWorkInstructionCountMap());
			}
		}
		//////////////////////////////////////////
		// Handler for Get Work
		else if (response instanceof GetWorkResponse) {
			GetWorkResponse workResponse = (GetWorkResponse) response;
			if (response.getStatus() == ResponseStatus.Success) {
				this.deviceManager.processGetWorkResponse(workResponse.getNetworkGuid(), workResponse.getWorkInstructions());
			}
		}
		//////////////////////////////////////////
		// Handler for WI Completion
		else if (response instanceof CompleteWorkInstructionResponse) {
			CompleteWorkInstructionResponse workResponse = (CompleteWorkInstructionResponse) response;
			if (response.getStatus() == ResponseStatus.Success) {
				this.deviceManager.processWorkInstructionCompletedResponse(workResponse.getWorkInstructionId());
			}
		} else {
			LOGGER.warn("Failed to handle response " + response);
		}
	}

	@Override
	public void handleMessage(UserSession session, MessageABC message) {
		//////////////////////////////////////////
		// Handler for Network Update
		if (message instanceof NetworkStatusMessage) {
			NetworkStatusMessage update = (NetworkStatusMessage) message;
			LOGGER.info("Processing Network Status update");
			this.deviceManager.updateNetwork(update.getNetwork());
		} else if (message instanceof LightLedsMessage) {
			LightLedsMessage msg = (LightLedsMessage) message;
			// check the message
			if (!LightLedsMessage.verifyCommandString(msg.getLedCommands())) {
				LOGGER.error("handleOtherMessage found bad LightLedsMessage");
			} else {
				LOGGER.info("Processing LightLedsMessage");
				String guidStr = msg.getNetGuidStr();
				NetGuid theGuid = new NetGuid(guidStr);
				this.deviceManager.lightSomeLeds(theGuid, msg.getDurationSeconds(), msg.getLedCommands());
			}
		}
	}

	@Override
	public ResponseABC handleRequest(UserSession session, RequestABC request) {
		LOGGER.info("Request received for processing: " + request);
		CommandABC command = null;
		ResponseABC response = null;

		if (request instanceof PingRequest) {
			command = new PingCommand(null, (PingRequest) request);
		}
		// check if matching command was found
		if (command == null) {
			LOGGER.warn("Unable to find matching command for request " + request + ". Ignoring request.");
			return null;
		}
		// execute command and generate response to be sent to client
		response = command.exec();
		if (response != null) {
			// automatically tie response to request
			response.setRequestId(request.getMessageId());
		} else {
			LOGGER.warn("No response generated for request " + request);
		}
		return response;
	}

	public void setWebClient(JettyWebSocketClient client) {
		this.client = client;
	}
}
