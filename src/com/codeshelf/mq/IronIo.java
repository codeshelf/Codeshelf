/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: file.java,v 1.1 2010/09/28 05:41:28 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.mq;

import io.iron.ironmq.Client;
import io.iron.ironmq.Cloud;
import io.iron.ironmq.Message;
import io.iron.ironmq.Queue;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */
public class IronIo {

	public static final String	PROJECT_ID			= "533717c400bf4c000500001e";
	public static final String	TOKEN				= "Lrzn73XweR5uNdWeNp65_ZMi_Ew";
	public static final String	DBX_CODE_QUEUE_NAME	= "DBX Codes";

	private static final Logger	LOGGER				= LoggerFactory.getLogger(IronIo.class);

	public IronIo() {

	}

	public static String getWebhookName() {
		return "https://mq-aws-us-east-1.iron.io/1/projects/" + PROJECT_ID + "/queues/" + DBX_CODE_QUEUE_NAME + "/messages/webhook?oauth=" + TOKEN;
	}

	public final String getMessage(String inMessageQueueName) {
		String result = null;
		Client client = new Client(PROJECT_ID, TOKEN, Cloud.ironAWSUSEast);
		Queue queue = client.queue(inMessageQueueName);
		Message msg;
		try {
			msg = queue.get();
			result = msg.getBody();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		return result;
	}

}
