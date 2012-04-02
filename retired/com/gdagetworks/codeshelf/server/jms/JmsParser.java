/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: JmsParser.java,v 1.1 2012/04/02 07:58:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//--------------------------------------------------------------------------
/**
 *  @author jeffw
 */

public final class JmsParser {

	public static final String	JMS_SERVER_PARAM		= "SERVER";
	public static final String	JMS_ACCOUNT_PARAM		= "ACCT";
	public static final String	JMS_PASSWORD_PARAM		= "PASS";
	public static final String	JMS_QUEUE_PARAM			= "QUEUE";
	public static final String	JMS_PORT_PARAM			= "PORT";

	public static final String	JMS_PORT				= "61616";

	private static final Log	LOGGER					= LogFactory.getLog(JmsParser.class);

	private static final int	EXPIRATION_TIME_MILLIS	= 7 * 24 * 60 * 60 * 1000;

	private JmsParser() {
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	private static void processMessages() {

		String hostname = "hostname";
		String accountId = "account";
		String password = "password";
		String queueName = "queuename";
		String portNum = "portnum";
		String tcpUrl = "tcp://" + hostname + ":" + portNum;

		Session session = null;
		Queue queue = null;
		try {
			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(accountId, password, tcpUrl);
			Connection connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			queue = session.createQueue(queueName);
		} catch (JMSException e) {
			LOGGER.error("", e);
		}

		if ((session != null) && (queue != null)) {
			
			try {
				QueueBrowser browser = session.createBrowser(queue);
				Enumeration<Message> enumerator = browser.getEnumeration();
				while (enumerator.hasMoreElements()) {
					Message message = (Message) enumerator.nextElement();

					if (message instanceof TextMessage) {
						TextMessage textMessage = (TextMessage) message;
					} else if (message instanceof MapMessage) {
						ActiveMQMapMessage notification = (ActiveMQMapMessage) message;
						String contentId = notification.getString("contentId");
						LOGGER.info("Content Id: " + contentId);
					} else {
						LOGGER.info("Received: " + message);
					}

				}
			} catch (JMSException e) {
				LOGGER.error("", e);
			}
		}
	}
}
