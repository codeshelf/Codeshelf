package com.codeshelf.email;

import java.util.Arrays;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import lombok.Getter;

public class OutboundMessage {
	@Getter
	InternetAddress[]	to;
	@Getter
	String				subject;
	@Getter
	String				body;

	public OutboundMessage(String to, String subject, String body) {
		super();
		try {
			this.to = InternetAddress.parse(to);
		} catch (AddressException e) {
			throw new RuntimeException("Failed to parse email address for outbound message", e);
		}
		this.subject = subject;
		this.body = body;
	}

	@Override
	public String toString() {
		return "[TO=" + Arrays.toString(to) + ", SUBJECT=" + subject + ", BODY=" + body + "]";
	}
}
