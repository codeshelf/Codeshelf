package com.codeshelf.security;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.codeshelf.email.EmailService;
import com.codeshelf.email.TemplateService;
import com.codeshelf.manager.User;

public class SecurityEmails {

	public static void sendAccountCreation(User newUser) {
		sendTokenEmail(TokenSessionService.getInstance().createAccountSetupToken(newUser),"account-setup");
	}

	public static void sendAccountReset(User user) {
		sendTokenEmail(TokenSessionService.getInstance().createAccountSetupToken(user),"account-reset");
	}

	public static void sendRecovery(User user) {
		sendTokenEmail(TokenSessionService.getInstance().createAccountRecoveryToken(user),"account-recovery");
	}

	private static void sendTokenEmail(TokenSession session, String template) {
		String token = session.getNewToken();
		try {
			token = URLEncoder.encode(token, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		Map<String,Object> context = new HashMap<String,Object>(); 
		context.put("token", token);
		
		String messageSubject = TemplateService.getInstance().load(template+"-email-subject", context);
		String messageBody = TemplateService.getInstance().load(template+"-email-body", context);
		EmailService.getInstance().send(session.getUser().getUsername(), messageSubject, messageBody);
	}
	
}
