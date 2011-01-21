/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ActiveMqManager.java,v 1.3 2011/01/21 05:12:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.jms;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.kahadaptor.KahaPersistenceAdapter;
import org.apache.activemq.transport.stomp.StompConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class ActiveMqManager {

	public static final String					DEFAULT_PICKTAGS_MESSAGE_QUEUE	= "PickTagMsgQueue";

	private static final Log					LOGGER							= LogFactory.getLog(ActiveMqManager.class);

	private static final String					ACTIVEMQ_BROKER_NAME			= "PickTagMsgBroker";
	private static final String					ACTIVEMQ_HOST					= "localhost";

	private static final String					STOMP_PREFIX					= "/queue/";

	private static BrokerService				mBrokerService;
	private static ActiveMQConnectionFactory	mConnectionFactory;
	private static Connection					mConnection;
	private static Session						mSession;
	private static Queue						mPickTagMsgQueue;
	private static boolean						mTransacted;

	private ActiveMqManager() {

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void setupBrokerService() {

		try {
			PersistentProperty userIDProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.ACTIVEMQ_USERID);
			PersistentProperty passwordProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.ACTIVEMQ_PASSWORD);
			PersistentProperty jmsProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.ACTIVEMQ_JMS_PORTNUM);
			PersistentProperty stompProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.ACTIVEMQ_STOMP_PORTNUM);
			if ((userIDProp != null) && (passwordProp != null) && (stompProp != null)) {

				String userid = userIDProp.getCurrentValueAsStr();
				String password = passwordProp.getCurrentValueAsStr();
				String jmsPort = jmsProp.getCurrentValueAsStr();
				String stompPort = stompProp.getCurrentValueAsStr();
				String tcpUrl = "tcp://" + ACTIVEMQ_HOST + ":" + jmsPort;
				String stompUrl = "stomp://" + ACTIVEMQ_HOST + ":" + stompPort;

				mBrokerService = new BrokerService();
				mBrokerService.setBrokerName(ACTIVEMQ_BROKER_NAME);
				mBrokerService.setUseShutdownHook(false);
				//mBrokerService.setPlugins(new BrokerPlugin[] { new JaasAuthenticationPlugin() });
				mBrokerService.setUseJmx(true);
				mBrokerService.setEnableStatistics(true);
				mBrokerService.addConnector(stompUrl);
				mBrokerService.addConnector(tcpUrl);

				PersistenceAdapter kahaDb = new KahaPersistenceAdapter();
				kahaDb.setDirectory(new File(Util.getApplicationDataDirPath() + System.getProperty("file.separator") + "AMQ"));
				mBrokerService.setPersistenceAdapter(kahaDb);
				mBrokerService.setSchedulerSupport(false);

				mBrokerService.start();

				mConnectionFactory = new ActiveMQConnectionFactory(userid, password, tcpUrl);
				mConnection = mConnectionFactory.createConnection();
				mConnection.start();
				mSession = mConnection.createSession(mTransacted, Session.AUTO_ACKNOWLEDGE);
				mPickTagMsgQueue = mSession.createQueue(DEFAULT_PICKTAGS_MESSAGE_QUEUE);
			}
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static boolean startBrokerService() {

		boolean result = false;

		if (mBrokerService == null) {
			setupBrokerService();
		}
		if ((mBrokerService != null) && (mConnectionFactory != null)) {
			try {
				mBrokerService.start();
				mConnection = mConnectionFactory.createConnection();
				mConnection.start();
				IncomingMessageProcessor incomingMessageProcessor = new IncomingMessageProcessor();
				mConnection.setExceptionListener(incomingMessageProcessor);
				//				MessageConsumer consumer = mSession.createConsumer(mDatoBlockKMsgQueue);
				//				consumer.setMessageListener(incomingMessageProcessor);
				result = true;

			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void stopBrokerService() {
		if (mBrokerService != null) {
			try {
				mBrokerService.stop();
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static Session getBrokerSession() {
		Session result = null;

		// Only return a session if the broker service is running.
		if ((mBrokerService != null) && (mBrokerService.isStarted())) {
			result = mSession;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public static Queue getPickTagMsgQueue() {
		Queue result = null;

		// Only return a session if the broker service is running.
		if ((mBrokerService != null) && (mBrokerService.isStarted())) {
			result = mPickTagMsgQueue;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void setServerFromSettings() {

		PersistentProperty activeMqProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.ACTIVEMQ_RUN);
		boolean shouldRun = activeMqProp.getCurrentValueAsBoolean();
		if (shouldRun) {
			if ((mBrokerService == null) || (!mBrokerService.isStarted())) {
				startBrokerService();
			}
		} else {
			if ((mBrokerService != null) && (mBrokerService.isStarted())) {
				stopBrokerService();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void sendJMSMessage() {
		try {
			//			TopicConnectionFactory conFactory = new ActiveMQConnectionFactory();
			//			conFactory.setProperty("imqBrokerHostName", "localhost"); 
			//			conFactory.setProperty("imqBrokerHostPort", "61616");

			MessageProducer producer = mSession.createProducer(mPickTagMsgQueue);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			TextMessage message = mSession.createTextMessage("Hello World!");
			LOGGER.info("Sending message: " + message.getText());
			producer.send(message);
		} catch (JMSException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void sendStompMessage() {
		try {
			HashMap<String, String> headers = new HashMap<String, String>();

			StompConnection connection = new StompConnection();
			PersistentProperty userIDProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.ACTIVEMQ_USERID);
			PersistentProperty passwordProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.ACTIVEMQ_PASSWORD);
			PersistentProperty stompProp = Util.getSystemDAO().findPersistentProperty(PersistentProperty.ACTIVEMQ_STOMP_PORTNUM);
			if ((userIDProp != null) && (passwordProp != null) && (stompProp != null)) {

				connection.open(ACTIVEMQ_HOST, stompProp.getCurrentValueAsInt());

				connection.connect(userIDProp.getCurrentValueAsStr(), passwordProp.getCurrentValueAsStr());
				//			StompFrame connect = connection.receive();
				//			if (!connect.getAction().equals(Stomp.Responses.CONNECTED)) {
				//				throw new Exception("Not connected");
				//			}

				connection.begin("tx1");
				headers.clear();
				headers.put("persistent", "true");
				connection.send(STOMP_PREFIX + DEFAULT_PICKTAGS_MESSAGE_QUEUE, "message1", "tx1", headers);
				connection.send(STOMP_PREFIX + DEFAULT_PICKTAGS_MESSAGE_QUEUE, "message2", "tx1", headers);
				connection.commit("tx1");

				//				connection.begin("tx2");
				//
				//				headers.clear();
				//				headers.put("transaction", "tx2");
				//				connection.subscribe(STOMP_PREFIX + DEFAULT_PICKTAGS_MESSAGE_QUEUE, Subscribe.AckModeValues.CLIENT, headers);
				//
				//				StompFrame message = connection.receive();
				//				LOGGER.info(message.getBody());
				//				connection.ack(message, "tx2");
				//
				//				message = connection.receive();
				//				LOGGER.info(message.getBody());
				//				connection.ack(message, "tx2");
				//
				//				connection.commit("tx2");

				connection.disconnect();
			}
		} catch (UnknownHostException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @author jeffw
	 */
	private static class IncomingMessageProcessor implements MessageListener, ExceptionListener {

		public synchronized void onException(JMSException inException) {
			LOGGER.error("JMS Exception occured.  Shutting down client.", inException);
		}

		public void onMessage(Message inMessage) {
			try {
				if (inMessage instanceof TextMessage) {
					TextMessage textMessage = (TextMessage) inMessage;
					LOGGER.info("Received message: " + textMessage.getText());
				} else if (inMessage instanceof MapMessage) {
					ActiveMQMapMessage notification = (ActiveMQMapMessage) inMessage;
					String contentId = notification.getString("contentId");
					LOGGER.info("Content Id: " + contentId);
				} else {
					LOGGER.info("Received: " + inMessage);
				}
			} catch (JMSException ex) {
				LOGGER.error("Error reading message: " + ex);
			}
		}
	}
}