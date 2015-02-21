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
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.EdiServiceStateEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */

@Entity
@DiscriminatorValue("IRONMQ")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class IronMqService extends EdiServiceABC {

	@Inject
	public static ITypedDao<IronMqService>	DAO;

	@Singleton
	public static class IronMqServiceDao extends GenericDaoABC<IronMqService> implements ITypedDao<IronMqService> {
		public final Class<IronMqService> getDaoClass() {
			return IronMqService.class;
		}
	}

	public static final String		IRONMQ_SERVICE_NAME	= "IRONMQ";
	public static final int 		MAX_NUM_MESSAGES = 100;
	public static final String		WI_QUEUE_NAME		= "CompletedWIs";

	// Are these the GoodEggs credentials?
	// public static final String		PROJECT_ID			= "533717c400bf4c000500001e";
	// public static final String		TOKEN				= "Lrzn73XweR5uNdWeNp65_ZMi_Ew";
	// Let's go with our new temporary credentials instead. Need configuration!
	// public static final String		PROJECT_ID			= "5408fee49393690009000010";
	// public static final String		TOKEN				= "T9yxS7H9vUE8ck15vCPJOT_iymY";
	public static final String		PROJECT_ID			= "";
	public static final String		TOKEN				= "";

	private static final Logger		LOGGER				= LoggerFactory.getLogger(IronMqService.class);


	@Transient
	private Counter exportCounter = MetricsService.addCounter(MetricsGroup.WSS,"exports.ironmq");

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

	public IronMqService() {
		this(new ClientProvider() {

			@Override
			public Client get(String projectId, String token) {
				return new Client(projectId, token, Cloud.ironAWSUSEast);
			}

		});
	}

	IronMqService(ClientProvider clientProvider) {
		this.clientProvider = clientProvider;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<IronMqService> getDao() {
		return DAO;
	}

	public final static void setDao(ITypedDao<IronMqService> dao) {
		IronMqService.DAO = dao;
	}

	public final String getServiceName() {
		return IRONMQ_SERVICE_NAME;
	}

	private final void setCredentials(String projectId,  String token) {
		IronMqService.Credentials credentials = new Credentials(projectId, token);
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String json = gson.toJson(credentials);
		setProviderCredentials(json);
	}

	public void storeCredentials(String projectId,  String token) {
		setCredentials(projectId, token);
		if(getHasCredentials()) {
			try {
				Optional<Queue> queue = getWorkInstructionQueue();
				if (queue.isPresent()) {
					queue.get().getSize();
					setServiceState(EdiServiceStateEnum.LINKED);
					LOGGER.warn("IronMqService is linked, will export work instructions");
				}
				else {
					LOGGER.warn("Unable to get queue or no credentials set");
					setServiceState(EdiServiceStateEnum.UNLINKED);
					LOGGER.warn("IronMqService is unlinked, will not export work instructions");
				}
			}
			catch(Exception e) {
				LOGGER.warn("Unable to connect to iron mq with credentials", e);
				setServiceState(EdiServiceStateEnum.UNLINKED);
				LOGGER.warn("IronMqService is unlinked, will not export work instructions");
			}
		}
		IronMqService.DAO.store(this); //This is the DAO the UI is listening to
	}

	public boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {

		return false;
	}

	public void sendWorkInstructionsToHost(final String exportMessage) throws IOException, IllegalStateException {
		Optional<Queue> queue = getWorkInstructionQueue();
		if (queue.isPresent()) {
			LOGGER.debug("attempting send exportMessage: " + exportMessage);
			queue.get().push(exportMessage);
			exportCounter.inc();
			LOGGER.debug("Sent work instructions to iron mq service");
		}
		else {
			LOGGER.debug("Unable to send work instruction, no credentials for IronMqService");
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
			for (int i = 0; i < ids.length; i++) { //A newer client allos for deleting messages in batch
				String id = ids[i];
				ironMqQueue.deleteMessage(id);
			}
			return bodies;
		} else {
			throw new IllegalStateException("No queue or credentials configured for IronMqService");
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

}
