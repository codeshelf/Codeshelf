package com.codeshelf.device;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.ws.client.CsClientEndpoint;
import com.codeshelf.ws.protocol.command.CommandABC;
import com.codeshelf.ws.protocol.command.PingCommand;
import com.codeshelf.ws.protocol.message.CheDisplayMessage;
import com.codeshelf.ws.protocol.message.CheStatusMessage;
import com.codeshelf.ws.protocol.message.SiteControllerOperationMessage;
import com.codeshelf.ws.protocol.message.IMessageProcessor;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.codeshelf.ws.protocol.message.NetworkStatusMessage;
import com.codeshelf.ws.protocol.message.PosConLightAddressesMessage;
import com.codeshelf.ws.protocol.message.PosConSetupMessage;
import com.codeshelf.ws.protocol.message.PropertyChangeMessage;
import com.codeshelf.ws.protocol.message.ScriptMessage;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest.ComputeWorkPurpose;
import com.codeshelf.ws.protocol.request.PingRequest;
import com.codeshelf.ws.protocol.request.RequestABC;
import com.codeshelf.ws.protocol.response.LinkRemoteCheResponse;
import com.codeshelf.ws.protocol.response.CompleteWorkInstructionResponse;
import com.codeshelf.ws.protocol.response.ComputeWorkResponse;
import com.codeshelf.ws.protocol.response.FailureResponse;
import com.codeshelf.ws.protocol.response.GenericDeviceResponse;
import com.codeshelf.ws.protocol.response.GetOrderDetailWorkResponse;
import com.codeshelf.ws.protocol.response.GetPutWallInstructionResponse;
import com.codeshelf.ws.protocol.response.InfoResponse;
import com.codeshelf.ws.protocol.response.InventoryUpdateResponse;
import com.codeshelf.ws.protocol.response.LoginResponse;
import com.codeshelf.ws.protocol.response.PalletizerItemResponse;
import com.codeshelf.ws.protocol.response.PalletizerRemoveOrderResponse;
import com.codeshelf.ws.protocol.response.PutWallPlacementResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.protocol.response.TapeLocationDecodingResponse;
import com.codeshelf.ws.protocol.response.VerifyBadgeResponse;
import com.codeshelf.ws.server.WebSocketConnection;
import com.google.inject.Inject;

public class SiteControllerMessageProcessor implements IMessageProcessor {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(SiteControllerMessageProcessor.class);

	CsClientEndpoint			clientEndpoint;

	private CsDeviceManager		deviceManager;

	@Inject
	public SiteControllerMessageProcessor(CsDeviceManager deviceManager, CsClientEndpoint clientEndpoint) {
		this.deviceManager = deviceManager;
		this.clientEndpoint = clientEndpoint;

		CsClientEndpoint.setMessageProcessor(this);
	}

	@Override
	public void handleResponse(WebSocketConnection session, ResponseABC response) {
		try {
			this.setDeviceContext(response);

			LOGGER.info("Response received: {}", response);
			if (response.getStatus() != ResponseStatus.Success) {
				LOGGER.warn("Response:{} failed for request:{} statusMsg: {}",
					response.getClass().getSimpleName(),
					response.getRequestId(),
					response.getStatusMessage());
				// LOGGER.warn("Request #" + response.getRequestId() + " failed: " + response.getStatusMessage());
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

						deviceManager.setTenantName(loginResponse.getTenantName());
						deviceManager.setFacilityDomainId(loginResponse.getFacilityDomainId());
						ContextLogging.setTenantNameAndFacilityId(deviceManager.getTenantName(), deviceManager.getFacilityDomainId());
						
						// DEV-582 hook up to AUTOSHRT parameter
						deviceManager.setAutoShortValue(loginResponse.isAutoShortValue());
						deviceManager.setPickInfoValue(loginResponse.getPickInfoValue());
						String pickMult = loginResponse.getPickMultValue();
						deviceManager.setPickMultValue(Boolean.parseBoolean(pickMult));
						String production = loginResponse.getProductionValue();
						deviceManager.setProductionValue(Boolean.parseBoolean(production));
						deviceManager.setContainerTypeValue(loginResponse.getContainerTypeValue());
						deviceManager.setScanTypeValue(loginResponse.getScanTypeValue());
						deviceManager.setSequenceKind(loginResponse.getSequenceKind());
						deviceManager.setOrdersubValue(loginResponse.getOrdersubValue());
						deviceManager.getRadioController().setProtocolVersion(loginResponse.getProtocol());
						deviceManager.setSiteControllerRole(loginResponse.getSiteControllerRole());
						// attached has the huge side effect of getting all CHEs and setting up device logic for them. Better have the config values first.
						deviceManager.attached(network);
					} else {
						LOGGER.error("loginResponse has no network");
					}
				}
				if (!attached) {
					LOGGER.warn("Failed to attach network: {} ", response);
					try {
						this.deviceManager.unattached();
						clientEndpoint.disconnect();
					} catch (IOException e) {
						LOGGER.error("Failed to disconnect client", e);
					}
				}
			}
			//////////////////////////////////////////
			// Handler for Verify Badge
			else if (response instanceof VerifyBadgeResponse) {
				VerifyBadgeResponse verifyBadgeResponse = (VerifyBadgeResponse) response;
				deviceManager.processVerifyBadgeResponse(verifyBadgeResponse);
			}
			//////////////////////////////////////////
			// Handler for Compute Work and Get Work
			else if (response instanceof ComputeWorkResponse) {
				ComputeWorkResponse computeWorkResponse = (ComputeWorkResponse) response;
				if (computeWorkResponse.getPurpose() == ComputeWorkPurpose.COMPUTING_WORK || computeWorkResponse.getPathChanged()) {
					if (response.getStatus() == ResponseStatus.Success) {
						this.deviceManager.processComputeWorkResponse(computeWorkResponse.getNetworkGuid(),
							computeWorkResponse.getTotalGoodWorkInstructions(),
							computeWorkResponse.getContainerToWorkInstructionCountMap(),
							computeWorkResponse.getReplenishRun());
					}
				} else {
					if (response.getStatus() == ResponseStatus.Success) {
						this.deviceManager.processGetWorkResponse(computeWorkResponse.getNetworkGuid(),
							computeWorkResponse.getWorkInstructions(),
							computeWorkResponse.getStatusMessage());
					}
				}
			}
			//////////////////////////////////////////
			// Handler for WI Completion
			else if (response instanceof CompleteWorkInstructionResponse) {
				CompleteWorkInstructionResponse workResponse = (CompleteWorkInstructionResponse) response;
				this.deviceManager.processWorkInstructionCompletedResponse(workResponse);
			}
			//////////////////////////////////////////
			// Handler for Get Order Detail Work-- LINE_SCAN work flow
			else if (response instanceof GetOrderDetailWorkResponse) {
				GetOrderDetailWorkResponse workResponse = (GetOrderDetailWorkResponse) response;
				if (response.getStatus() == ResponseStatus.Success) {
					LOGGER.info("GetOrderDetailWorkResponse received: success. Passing through to deviceManager");
					this.deviceManager.processOrderDetailWorkResponse(workResponse.getNetworkGuid(),
						workResponse.getWorkInstructions(),
						workResponse.getStatusMessage());
				} else {
					LOGGER.info("GetOrderDetailWorkResponse received: not success. No action.");
				}
			}
			//////////////////////////////////////////
			// Handler for Get Order Detail Work-- LINE_SCAN work flow
			else if (response instanceof GetPutWallInstructionResponse) {
				GetPutWallInstructionResponse wallResponse = (GetPutWallInstructionResponse) response;
				if (wallResponse.getStatus() == ResponseStatus.Success) {
					LOGGER.info("GetPutWallInstructionResponse received: success. Passing through to deviceManager");
					this.deviceManager.processPutWallInstructionResponse(wallResponse);
				} else {
					LOGGER.info("GetPutWallInstructionResponse received: not success. No action.");
				}
			}

			else if (response instanceof InventoryUpdateResponse) {
				InventoryUpdateResponse inventoryScanResponse = (InventoryUpdateResponse) response;
				if (response.getStatus() == ResponseStatus.Success) {
					this.deviceManager.processInventoryScanRespose(inventoryScanResponse.getStatusMessage());
				}
			}

			else if (response instanceof PutWallPlacementResponse) {
			}

			else if (response instanceof LinkRemoteCheResponse) {
				LinkRemoteCheResponse linkResponse = (LinkRemoteCheResponse) response;
				if (response.getStatus() == ResponseStatus.Success) {
					this.deviceManager.processCheLinkResponse(linkResponse.getNetworkGuid(),
						linkResponse.getCheName(),
						linkResponse.getLinkedCheGuid(),
						linkResponse.getLinkedCheName());
				}
			}

			else if (response instanceof TapeLocationDecodingResponse) {
				TapeLocationDecodingResponse devodingResponse = (TapeLocationDecodingResponse) response;
				if (response.getStatus() == ResponseStatus.Success) {
					this.deviceManager.processTapeLocationDecodingResponse(devodingResponse.getNetworkGuid(),
						devodingResponse.getDecodedLocation());
				}
			}

			else if (response instanceof InfoResponse) {
				InfoResponse infoResponse = (InfoResponse) response;
				if (response.getStatus() == ResponseStatus.Success) {
					this.deviceManager.processInfoResponse(infoResponse.getNetworkGuid(), infoResponse.getInfo());
				}
			}

			else if (response instanceof PalletizerItemResponse) {
				PalletizerItemResponse palletizerResponse = (PalletizerItemResponse) response;
				if (response.getStatus() == ResponseStatus.Success) {
					this.deviceManager.processPalletizerItemResponse(palletizerResponse.getNetworkGuid(),
						palletizerResponse.getInfo());
				}
			}

			else if (response instanceof PalletizerRemoveOrderResponse) {
				PalletizerRemoveOrderResponse palletizerRemoveResponse = (PalletizerRemoveOrderResponse) response;
				if (response.getStatus() == ResponseStatus.Success) {
					this.deviceManager.processPalletizerRemoveResponse(palletizerRemoveResponse.getNetworkGuid(),
						palletizerRemoveResponse.getInfo());
				}
			}

			else if (response instanceof GenericDeviceResponse) {
			}

			// Handle server-side errors
			else if (response instanceof FailureResponse) {
				FailureResponse failureResponse = (FailureResponse) response;
				this.deviceManager.processFailureResponse(failureResponse);
			}

			else {
				LOGGER.warn("Failed to handle response " + response);
			}
		} finally {
			this.clearDeviceContext();
		}
	}

	@Override
	public void handleMessage(WebSocketConnection session, MessageABC message) {
		try {
			this.setDeviceContext(message);

			//////////////////////////////////////////
			// Handler for Network Update
			if (message instanceof NetworkStatusMessage) {
				NetworkStatusMessage update = (NetworkStatusMessage) message;
				LOGGER.info("Processing Network Status update");
				this.deviceManager.updateNetwork(update.getNetwork());
			} else if (message instanceof LedInstrListMessage) {
				this.deviceManager.lightSomeLeds(((LedInstrListMessage) message).getInstructions());
			} else if (message instanceof CheDisplayMessage) {
				CheDisplayMessage msg = (CheDisplayMessage) message;
				String guidStr = msg.getNetGuidStr();
				NetGuid theGuid = new NetGuid(guidStr);
				this.deviceManager.processDisplayCheMessage(theGuid, msg.getLine1(), msg.getLine2(), msg.getLine3(), msg.getLine4());
			} else if (message instanceof PosControllerInstr) {
				PosControllerInstr msg = (PosControllerInstr) message;
				this.deviceManager.processPosConControllerMessage(msg, false);
			} else if (message instanceof PosControllerInstrList) {
				PosControllerInstrList msg = (PosControllerInstrList) message;
				this.deviceManager.processPosConControllerListMessage(msg);
			} else if (message instanceof OrderLocationFeedbackMessage) {
				OrderLocationFeedbackMessage msg = (OrderLocationFeedbackMessage) message;
				this.deviceManager.processOrderLocationFeedbackMessage(msg);
			} else if (message instanceof CheStatusMessage) {
				CheStatusMessage msg = (CheStatusMessage) message;
				LOGGER.info("Setup-state initialization received for Che: " + msg.getNetGuidStr());
				this.deviceManager.processSetupStateMessage(msg.getNetGuidStr(), msg.getContainerPositions());
			} else if (message instanceof ScriptMessage) {
				ScriptMessage msg = (ScriptMessage) message;
				new ScriptSiteRunner(deviceManager).runScript(msg);
			} else if (message instanceof PosConSetupMessage) {
				PosConSetupMessage msg = (PosConSetupMessage) message;
				this.deviceManager.processPosConSetupMessage(msg);
			} else if (message instanceof PosConLightAddressesMessage) {
				PosConLightAddressesMessage msg = (PosConLightAddressesMessage) message;
				this.deviceManager.processPosConLightAddresses(msg);
			} else if (message instanceof SiteControllerOperationMessage) {
				switch(((SiteControllerOperationMessage)message).getTask()){
					case DISCONNECT_DEVICES:
						this.deviceManager.disconnected();
						break;
					case SHUTDOWN:
						LOGGER.warn("Shut down Site Controller");
						System.exit(0);
						break;
				}
			} else if (message instanceof PropertyChangeMessage) {
				updateProperty((PropertyChangeMessage)message);
			}
		} finally {
			this.clearDeviceContext();
		}
	}

	@Override
	public ResponseABC handleRequest(WebSocketConnection session, RequestABC request) {
		try {
			this.setDeviceContext(request);

			// DEV-1261 was info before v24. Most requests lead to informative logging downstream, so this is a poor duplicate.
			// But we run the risk of losing information about new request types that do not log well. We might need to add
			// a RequestABC function asking just that: "do you log well? or do you want this lousy logging?"
			LOGGER.debug("Request received for processing: {}", request);
			@SuppressWarnings("rawtypes")
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
			response = command.run();
			if (response != null) {
				// automatically tie response to request
				response.setRequestId(request.getMessageId());
			} else {
				LOGGER.warn("No response generated for request " + request);
			}
			return response;
		} finally {
			this.clearDeviceContext();
		}
	}

	private void setDeviceContext(MessageABC message) {
		INetworkDevice device = deviceManager.getDevice(message.getDeviceIdentifier());
		if (device != null) {
			ContextLogging.setNetGuid(device.getGuid());
		}
	}

	private void clearDeviceContext() {
		ContextLogging.clearNetGuid();
	}
	
	private void updateProperty(PropertyChangeMessage message){
		boolean siteControllerProperty = true;
		String value = message.getValue();
		switch (message.getType()){
			case AUTOSHRT:
				deviceManager.setAutoShortValue(Boolean.parseBoolean(value));
				break;
			case PICKINFO:
				deviceManager.setPickInfoValue(value);
				break;
			case PICKMULT:
				deviceManager.setPickMultValue(Boolean.parseBoolean(value));
				break;
			case CNTRTYPE:
				deviceManager.setContainerTypeValue(value);
				break;
			case SCANPICK:
				deviceManager.setScanTypeValue(value);
				break;
			case WORKSEQR:
				deviceManager.setSequenceKind(value);
				break;
			case PRODUCTION:
				deviceManager.setProductionValue(Boolean.parseBoolean(value));
				break;
			case ORDERSUB:
				deviceManager.setOrdersubValue(value);
				break;
			case PROTOCOL:
				break;
			default:
				siteControllerProperty = true;
		}
		if (siteControllerProperty){
			LOGGER.info("Updating SiteController-important property {} to {}", message.getType(), value);
		}
	}
}
