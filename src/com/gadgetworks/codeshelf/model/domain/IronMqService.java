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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.edi.ICsvAislesFileImporter;
import com.gadgetworks.codeshelf.edi.ICsvCrossBatchImporter;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderLocationImporter;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "edi_service")
@DiscriminatorValue("IRONMQ")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
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

	public static final String		IRONMQ_SERVICE_NAME	= "IRONMQ";

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

	private static final String		TIME_FORMAT			= "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private static final Integer	DOMAINID_POS		= 0;
	private static final Integer	TYPE_POS			= 1;
	private static final Integer	STATUS_POS			= 2;
	private static final Integer	ORDERGROUPID_POS	= 3;
	private static final Integer	ORDERID_POS			= 4;
	private static final Integer	CONTAINERID_POS		= 5;
	private static final Integer	ITEMID_POS			= 6;
	private static final Integer	LOCATIONID_POS		= 7;
	private static final Integer	PICKERID_POS		= 8;
	private static final Integer	PLAN_QTY_POS		= 9;
	private static final Integer	ACT_QTY_POS			= 10;
	private static final Integer	ASSIGNED_POS		= 11;
	private static final Integer	STARTED_POS			= 12;
	private static final Integer	COMPLETED_POS		= 13;
	private static final Integer	WI_ATTR_COUNT		= 14;											// The total count of these attributes.

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

	public final void setCredentials(String projectId,  String token) {
		IronMqService.Credentials credentials = new Credentials(projectId, token);
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		String json = gson.toJson(credentials);
		setProviderCredentials(json);
	}
	
	public final void storeCredentials(String projectId,  String token) {
		setCredentials(projectId, token);
		if (getHasCredentials()) {
			setServiceStateEnum(EdiServiceStateEnum.LINKED);
		} else {
			setServiceStateEnum(EdiServiceStateEnum.UNLINKED);
		}
		EdiServiceABC.DAO.store(this); //This is what the UI is listening to
	}

	@Override
	@JsonProperty
	public boolean getHasCredentials() {
		// If the credentials are empty, don't bother. Could check the link, but we are not maintaining that well currently.
		String theCredentials = getProviderCredentials();
		if (Strings.isNullOrEmpty(theCredentials) || theCredentials.length() < 55) {
			return false; // this is a Json encoding  of two credentials. Much longer if valid...
		}
		else {
			return true;
		}
	}

	public final boolean getUpdatesFromHost(ICsvOrderImporter inCsvOrderImporter,
		ICsvOrderLocationImporter inCsvOrderLocationImporter,
		ICsvInventoryImporter inCsvInventoryImporter,
		ICsvLocationAliasImporter inCsvLocationsImporter,
		ICsvCrossBatchImporter inCsvCrossBatchImporter,
		ICsvAislesFileImporter inCsvAislesFileImporter) {

		return false;
	}

	public final void sendWorkInstructionsToHost(final List<WorkInstruction> inWiList) {

		// If the credentials are empty, don't bother. Could check the link, but we are not maintaining that well currently.
		String theCredentials = getProviderCredentials();

		if (theCredentials == null || theCredentials.length() < 55)
			return; // this is a Json encoding  of two credentials. Much longer if valid

		// Convert the WI into a CSV string.
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		String[] properties = new String[WI_ATTR_COUNT];
		properties[DOMAINID_POS] = "domainId";
		properties[TYPE_POS] = "type";
		properties[STATUS_POS] = "status";
		properties[ORDERGROUPID_POS] = "orderGroupId";
		properties[ORDERID_POS] = "orderId";
		properties[CONTAINERID_POS] = "containerId";
		properties[ITEMID_POS] = "itemId";
		properties[LOCATIONID_POS] = "locationId";
		properties[PICKERID_POS] = "pickerId";
		properties[PLAN_QTY_POS] = "planQuantity";
		properties[ACT_QTY_POS] = "actualQuantity";
		properties[ASSIGNED_POS] = "assigned";
		properties[STARTED_POS] = "started";
		properties[COMPLETED_POS] = "completed";
		csvWriter.writeNext(properties);

		for (WorkInstruction wi : inWiList) {

			LocationAlias locAlias = null;
			if (wi.getLocation().getAliases().size() > 0) {
				locAlias = wi.getLocation().getAliases().get(0);
			}

			properties = new String[WI_ATTR_COUNT];
			properties[DOMAINID_POS] = wi.getDomainId();
			properties[TYPE_POS] = wi.getTypeEnum().toString();
			properties[STATUS_POS] = wi.getStatusEnum().toString();

			// groups are optional!
			String groupStr = "";
			OrderGroup theGroup = wi.getParent().getParent().getOrderGroup();
			if (theGroup != null)
				groupStr = theGroup.getDomainId();
			properties[ORDERGROUPID_POS] = groupStr;

			properties[ORDERID_POS] = wi.getParent().getOrderId();
			properties[CONTAINERID_POS] = wi.getContainerId();
			properties[ITEMID_POS] = wi.getItemId();
			if (locAlias != null) {
				properties[LOCATIONID_POS] = locAlias.getDomainId();
			} else {
				properties[LOCATIONID_POS] = wi.getLocationId();
			}
			properties[PICKERID_POS] = wi.getPickerId();
			properties[PLAN_QTY_POS] = Integer.toString(wi.getPlanQuantity());
			properties[ACT_QTY_POS] = Integer.toString(wi.getActualQuantity());
			properties[ASSIGNED_POS] = new SimpleDateFormat(TIME_FORMAT).format(wi.getAssigned());
			properties[STARTED_POS] = new SimpleDateFormat(TIME_FORMAT).format(wi.getStarted());
			properties[COMPLETED_POS] = new SimpleDateFormat(TIME_FORMAT).format(wi.getCompleted());
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

		Credentials credentials = getCredentials();
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

		Credentials credentials = getCredentials();
		Client client = new Client(credentials.getProjectId(), credentials.getToken(), Cloud.ironAWSUSEast);
		Queue queue = client.queue(inQueueName);
		try {
			queue.push(inMessage);
		} catch (IOException e) {
			LOGGER.error("IOException in ironMQ sendMessage", e);
		}
	}

	private Credentials getCredentials() {
		Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		Credentials credentials = mGson.fromJson(getProviderCredentials(), Credentials.class);
		return credentials;
	}

}
