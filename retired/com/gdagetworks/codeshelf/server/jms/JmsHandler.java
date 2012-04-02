/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: JmsHandler.java,v 1.1 2012/04/02 07:58:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jeffw
 *
 */
public final class JmsHandler implements IControlMessageTransport, MessageListener {

	private static final Log		LOGGER	= LogFactory.getLog(JmsHandler.class);
	private static JmsHandler		mJmsHandler;

	private ControlMessageHandler	mMessageHandler;

	public JmsHandler(final ControlMessageHandler inControlMessageHandler) {
		mMessageHandler = inControlMessageHandler;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void startJmsHandler() {
		ControlMessageHandler messageHandler = new ControlMessageHandler();
		mJmsHandler = new JmsHandler(messageHandler);
		mJmsHandler.startTransport();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IControlMessageTransport#startTransport()
	 */
	public void startTransport() {

		String hostname = "localhost";
		String accountId = "codeshelf";
		String password = "codeshelf";
		String topicName = "ATOP";
		String portNum = "61616";
		String tcpUrl = "tcp://" + hostname + ":" + portNum;
		String subscriberName = "codeshelf";
		String clientID = "jmsHandler";

		try {
			TopicConnectionFactory connectionFactory = new ActiveMQConnectionFactory(accountId, password, tcpUrl);
			TopicConnection connection = connectionFactory.createTopicConnection();
			connection.setClientID(clientID);
			connection.start();
			TopicSession subSession = connection.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
			TopicSession pubSession = connection.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
			Topic topic = subSession.createTopic(topicName);

			if ((subSession != null) && (topic != null)) {

				TopicSubscriber subscriber = subSession.createDurableSubscriber(topic, subscriberName);
				subscriber.setMessageListener(this);

				TopicPublisher publisher = pubSession.createPublisher(topic);
				MapMessage message = pubSession.createMapMessage();
				message.setInt("messageID", (int) 123);
				message.setShort("protocol", (short) 1);
				byte[] dataBytes = "This is a test".getBytes();
				message.setBytes("dataBytes", dataBytes);
				
				publisher.publish(message);

			}
		} catch (JMSException e) {
			LOGGER.error("", e);
		}

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IControlMessageTransport#pauseTransport()
	 */
	public void pauseTransport() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IControlMessageTransport#stopTransport()
	 */
	public void stopTransport() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IControlMessageTransport#inputControlMessage(com.gadgetworks.codeshelf.server.ControlMessage)
	 */
	public void inputControlMessage(ControlMessage inControlMessage) {

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IControlMessageTransport#outputControlMessage(com.gadgetworks.codeshelf.server.ControlMessage)
	 */
	public void outputControlMessage(ControlMessage inControlMessage) {

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IControlMessageTransport#getControlMessageHandler()
	 */
	public ControlMessageHandler getControlMessageHandler() {
		return mMessageHandler;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IControlMessageTransport#setControlMessageHandler(com.gadgetworks.codeshelf.server.ControlMessageHandler)
	 */
	public void setControlMessageHandler(ControlMessageHandler inMessageHandler) {
		mMessageHandler = inMessageHandler;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	public void onMessage(Message inJmsMessage) {

		if (inJmsMessage instanceof TextMessage) {
			TextMessage textMessage = (TextMessage) inJmsMessage;
		} else if (inJmsMessage instanceof MapMessage) {
			try {
				ActiveMQMapMessage notification = (ActiveMQMapMessage) inJmsMessage;
				String messageId = notification.getString("messageId");
				ControlProtocolEnum protocol = ControlProtocolEnum.geControlProtocolEnum(notification.getInt("protocol"));
				byte[] dataBytes = notification.getBytes("dataBytes");
				ControlMessage controlMessage = new ControlMessage(messageId, protocol, dataBytes);
				mMessageHandler.receiveControlMessage(controlMessage);
			} catch (JMSException e) {
				LOGGER.error("", e);
			}
		} else {
		}

	}
}
