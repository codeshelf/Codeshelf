/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: file.java,v 1.1 2010/09/28 05:41:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import io.iron.ironmq.Client;
import io.iron.ironmq.Cloud;
import io.iron.ironmq.Message;
import io.iron.ironmq.Queue;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "edi_service")
@DiscriminatorValue("IRONMQ")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class IronMqService extends EdiServiceABC {

	@Inject
	public static ITypedDao<IronMqService>	DAO;

	@Singleton
	public static class IronMqServiceDao extends GenericDaoABC<IronMqService> implements ITypedDao<IronMqService> {
		@Inject
		public IronMqServiceDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<IronMqService> getDaoClass() {
			return IronMqService.class;
		}
	}

	public static final String	IRONMQ_SERVICE_NAME	= "IRONMQ";

	public static final String	WI_QUEUE_NAME		= "CompletedWIs";
	public static final String	PROJECT_ID			= "533717c400bf4c000500001e";
	public static final String	TOKEN				= "Lrzn73XweR5uNdWeNp65_ZMi_Ew";

	private static final Logger	LOGGER				= LoggerFactory.getLogger(IronMqService.class);

	public class Credentials {

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

	public final ITypedDao<IronMqService> getDao() {
		return DAO;
	}

	public final String getServiceName() {
		return IRONMQ_SERVICE_NAME;
	}

	public final boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter) {

		return false;
	}

	public final void sendWorkInstructionsToHost(final List<WorkInstruction> inWiList) {
		// Convert the WI into a CSV string.
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		String[] properties = new String[11];
		properties[0] = "domainId";
		properties[1] = "type";
		properties[2] = "status";
		properties[3] = "containerId";
		properties[3] = "itemId";
		properties[4] = "locationId";
		properties[5] = "pickerId";
		properties[6] = "planQuantity";
		properties[7] = "actualQuantity";
		properties[8] = "assigned";
		properties[9] = "started";
		properties[10] = "completed";
		csvWriter.writeNext(properties);

		for (WorkInstruction wi : inWiList) {
			properties = new String[11];
			properties[0] = wi.getDomainId();
			properties[1] = wi.getTypeEnum().toString();
			properties[2] = wi.getStatusEnum().toString();
			properties[3] = wi.getContainerId();
			properties[3] = wi.getItemId();
			properties[4] = wi.getLocationId();
			properties[5] = wi.getPickerId();
			properties[6] = Integer.toString(wi.getPlanQuantity());
			properties[7] = Integer.toString(wi.getActualQuantity());
			properties[8] = new SimpleDateFormat("ddMMMyy HH:mm").format(wi.getAssigned());
			properties[9] = new SimpleDateFormat("ddMMMyy HH:mm").format(wi.getStarted());
			properties[10] = new SimpleDateFormat("ddMMMyy HH:mm").format(wi.getCompleted());
			csvWriter.writeNext(properties);
		}
		try {
			csvWriter.close();
			sendMessage(WI_QUEUE_NAME, stringWriter.toString());
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inQueueName
	 * @return
	 */
	private String getMessage(String inQueueName) {
		String result = null;

		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Credentials credentials = mGson.fromJson(getProviderCredentials(), Credentials.class);
		Client client = new Client(credentials.getProjectId(), credentials.getToken(), Cloud.ironAWSUSEast);
		Queue queue = client.queue(inQueueName);
		Message msg;
		try {
			msg = queue.get();
			result = msg.getBody();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inQueueName
	 * @param inMessage
	 */
	private void sendMessage(final String inQueueName, final String inMessage) {

		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Credentials credentials = mGson.fromJson(getProviderCredentials(), Credentials.class);
		Client client = new Client(credentials.getProjectId(), credentials.getToken(), Cloud.ironAWSUSEast);
		Queue queue = client.queue(inQueueName);
		try {
			queue.push(inMessage);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

}
