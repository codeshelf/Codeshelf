/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: file.java,v 1.1 2010/09/28 05:41:28 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import io.iron.ironmq.Client;
import io.iron.ironmq.Cloud;
import io.iron.ironmq.Message;
import io.iron.ironmq.Messages;
import io.iron.ironmq.Queue;

import java.io.IOException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Max;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codeshelf.edi.IEdiExportGateway;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.EdiGatewayStateEnum;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author jeffw
 *
 */

@Entity
@DiscriminatorValue("IRONMQ")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class IronMqGateway extends EdiGateway implements IEdiExportGateway {

	public static final String		IRONMQ_SERVICE_NAME	= "IRONMQ";
	public static final int 		MAX_NUM_MESSAGES = 100;
	public static final String		WI_QUEUE_NAME		= "CompletedWIs";

	public static final String		PROJECT_ID			= "";
	public static final String		TOKEN				= "";

	private static final Logger		LOGGER				= LoggerFactory.getLogger(IronMqGateway.class);

	@Transient
	private Counter exportCounter;

	@Transient
	private ClientProvider	clientProvider;

	static interface ClientProvider {

		Client get(String projectId, String token);
	}

	public static class Credentials {

		public Credentials() {
			this("", "");
		}
		
		public Credentials(final String inProjectId, final String inToken) {
			mProjectId = inProjectId;
			mToken = inToken;
		}

		@Getter
		@Setter
		@Accessors(prefix = "m")
		@SerializedName(value = "projectId")
		@Expose
		private String	mProjectId;

		@Getter
		@Setter
		@Accessors(prefix = "m")
		@SerializedName(value = "token")
		@Expose
		private String	mToken;
	}

	public IronMqGateway() {
		this(new ClientProvider() {

			@Override
			public Client get(String projectId, String token) {
				return new Client(projectId, token, Cloud.ironAWSUSEast);
			}

		});
	}

	IronMqGateway(ClientProvider clientProvider) {
		super();
		this.setTransportType(EdiTransportType.IRONMQ);
		this.clientProvider = clientProvider;
		exportCounter = MetricsService.getInstance().createCounter(MetricsGroup.WSS,"exports.ironmq");

	}

	public final String getServiceName() {
		return IRONMQ_SERVICE_NAME;
	}
	
	@Override
	public boolean testConnection() {
		Optional<Queue> queue = getWorkInstructionQueue();
		return queue.isPresent();
	}

	private final void setCredentials(String projectId,  String token, String activeStr) {
		IronMqGateway.Credentials credentials = new Credentials(projectId, token);
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String json = gson.toJson(credentials);
		setProviderCredentials(json);
		if (activeStr == null || activeStr.isEmpty() || !"true".equalsIgnoreCase(activeStr)){
			setActive(false);
		} else {
			setActive(true);
		}
	}

	public void storeCredentials(String projectId,  String token, String activeStr) {
		setCredentials(projectId, token, activeStr);
		if(getHasCredentials()) {
			try {
				Optional<Queue> queue = getWorkInstructionQueue();
				if (queue.isPresent()) {
					queue.get().getInfoAboutQueue();
					setGatewayState(EdiGatewayStateEnum.LINKED);
					LOGGER.warn("IronMqGateway is linked, will export work instructions");
				}
				else {
					LOGGER.warn("Unable to get queue or no credentials set");
					setGatewayState(EdiGatewayStateEnum.UNLINKED);
					LOGGER.warn("IronMqGateway is unlinked, will not export work instructions");
				}
			}
			catch(Exception e) {
				LOGGER.warn("Unable to connect to iron mq with credentials", e);
				setGatewayState(EdiGatewayStateEnum.UNLINKED);
				LOGGER.warn("IronMqGateway is unlinked, will not export work instructions");
			}
		}
		IronMqGateway.staticGetDao().store(this); //This is the DAO the UI is listening to
	}

	public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {

		return false;
	}
	
	@Override
	public void transportWiFinished(String wiOrderId, String wiCheGuid, String exportMessage) throws IOException {
		Optional<Queue> queue = getWorkInstructionQueue();
		if (queue.isPresent()) {
			LOGGER.debug("attempting send exportMessage: " + exportMessage);
			queue.get().push(exportMessage);
			exportCounter.inc();
			LOGGER.debug("Sent work instructions to iron mq service");
		}
		else {
			LOGGER.debug("Unable to send work instruction, no credentials for IronMqGateway");
		}
	}

	String[] consumeMessages(@Max(100) int numberOfMessages, int timeoutInSeconds) throws IOException {
		Optional<Queue> queue = getWorkInstructionQueue();
		if (queue.isPresent()) {
			Queue ironMqQueue = queue.get();
			Messages messages = ironMqQueue.get(numberOfMessages, timeoutInSeconds);
			Message[] messageArray = messages.getMessages();
			String[] bodies = new String[messageArray.length];
			String[] ids = new String[messageArray.length];
			for (int i = 0; i < messageArray.length; i++) {
				Message message = messageArray[i];
				bodies[i] = message.getBody();
				ids[i] = message.getId();
			}
			for (int i = 0; i < ids.length; i++) { //A newer client allows for deleting messages in batch
				String id = ids[i];
				ironMqQueue.deleteMessage(id);
			}
			return bodies;
		} else {
			throw new IllegalStateException("No queue or credentials configured for IronMqGateway");
		}
	}

	private Optional<Queue> getWorkInstructionQueue() {
		if (getHasCredentials()) {
			Credentials theCredentials = getCredentials();
			Client client = clientProvider.get(theCredentials.getProjectId(), theCredentials.getToken());
			Queue queue = client.queue(WI_QUEUE_NAME);
			LOGGER.debug("Retrieving IronMQ Queue: " + WI_QUEUE_NAME + " for project: " + theCredentials.getProjectId());
			return Optional.of(queue);
		} else { 
			return Optional.absent();
		}
	}

	private Credentials getCredentials() {
		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Credentials credentials = mGson.fromJson(getProviderCredentials(), Credentials.class);
		return credentials;
	}

	@Override
	public boolean getHasCredentials() {
		Credentials theCredentials = getCredentials();
		return (theCredentials != null
				&& !Strings.isNullOrEmpty(theCredentials.getProjectId())
				&& !Strings.isNullOrEmpty(theCredentials.getToken()));
	}


	@Override
	public FileExportReceipt transportOrderOnCartFinished(String wiOrderId, String wiCheGuid, String message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void transportOrderOnCartRemoved(String wiOrderId, String wiCheGuid, String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FileExportReceipt transportOrderOnCartAdded(String wiOrderId, String wiCheGuid, String message) {
		return null;
		// TODO Auto-generated method stub
		
	}
}
