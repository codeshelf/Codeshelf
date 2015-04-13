package com.codeshelf.email;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.AbstractCodeshelfExecutionThreadService;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class EmailService extends AbstractCodeshelfExecutionThreadService {
	private static final Logger				LOGGER					= LoggerFactory.getLogger(EmailService.class);

	public final static int					EMAIL_QUEUE_SIZE		= 100;
	public final static String				SHUTDOWN_MESSAGE		= "***POISON***";	// body of message to enqueue for shutdown
	public final static int					SHUTDOWN_WAIT_SECONDS	= 30;
	public final static int					OFFER_WAIT_SECONDS		= 30;

	public final static boolean				MAIL_SMTP_AUTH			= true;
	public final static boolean				MAIL_STARTTLS_ENABLE	= true;
	public final static String				MAIL_SMTP_HOST			= "smtp.gmail.com";
	public final static int					MAIL_SMTP_PORT			= 587;

	private Session							session;
	private InternetAddress					from;
	private PasswordAuthentication			authentication;
	private BlockingQueue<OutboundMessage>	pending					= null;

	@Inject
	private static EmailService				theInstance;

	private EmailService() {
	}

	public final static EmailService getInstance() {
		return theInstance;
	}

	public final static void setInstance(EmailService instance) {
		// for testing
		theInstance = instance;
	}

	@Override
	protected void startUp() throws Exception {
		String username = System.getProperty("mail.username");
		String password = System.getProperty("mail.password");
		this.from = new InternetAddress(username);

		Properties sendConfig = new Properties();
		sendConfig.put("mail.smtp.auth", MAIL_SMTP_AUTH ? "true" : "false");
		sendConfig.put("mail.smtp.starttls.enable", MAIL_STARTTLS_ENABLE ? "true" : "false");
		sendConfig.put("mail.smtp.host", MAIL_SMTP_HOST);
		sendConfig.put("mail.smtp.port", Integer.toString(MAIL_SMTP_PORT));

		if (Strings.isNullOrEmpty(password)) {
			this.session = null;
		} else {
			this.authentication = new PasswordAuthentication(username, password);
			Authenticator authenticator = new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return authentication;
				}
			};
			this.session = Session.getInstance(sendConfig, authenticator);
		}

		pending = new ArrayBlockingQueue<OutboundMessage>(EMAIL_QUEUE_SIZE);
	}

	@Override
	protected void shutDown() throws Exception {
		this.session = null;
	}

	@Override
	protected void run() throws Exception {
		boolean shutdownRequested = false;
		while (isRunning() && !shutdownRequested) {
			OutboundMessage outboundMessage = null;
			try {
				outboundMessage = this.pending.take();
			} catch (InterruptedException e1) {
			}
			if (outboundMessage != null) {
				if (outboundMessage.getBody().equals(SHUTDOWN_MESSAGE)) {
					shutdownRequested = true;
				} else {
					sendEmail(outboundMessage);
				}
			}
		}
	}

	@Override
	protected void triggerShutdown() {
		OutboundMessage poison = new OutboundMessage(null, null, SHUTDOWN_MESSAGE);
		boolean result = false;
		try {
			result = this.pending.offer(poison, SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Timeout trying to stop EmailService", e);
		}
		if (!result) {
			LOGGER.error("Did not stop EmailService (queue full)");
		}
	}

	private void sendEmail(OutboundMessage outboundMessage) {
		if (session == null) {
			LOGGER.error("Not actually configured to send email: {}", outboundMessage.toString());
		} else {
			try {
				Message message = new MimeMessage(session);
				message.setFrom(this.from);
				message.setRecipients(Message.RecipientType.TO, outboundMessage.getTo());
				message.setSubject(outboundMessage.getSubject());
				message.setText(outboundMessage.getBody());
				Transport.send(message);
				LOGGER.error("Sent email: {}", outboundMessage.toString());
			} catch (MessagingException e) {
				LOGGER.error("Failed to send email: {}", outboundMessage.toString(), e);
			}
		}
	}

	public boolean send(String to, String subject, String body) {
		OutboundMessage message = new OutboundMessage(to, subject, body);
		boolean result = false;
		try {
			result = this.pending.offer(message, OFFER_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.warn("Interrupted enqueuing email", e);
		}
		if (!result) {
			LOGGER.error("Failed to enqueue email: {}", message.toString());
		}
		return result;
	}
}
