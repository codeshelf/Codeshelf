package com.codeshelf.manager.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.yaml.snakeyaml.Yaml;

import com.codeshelf.manager.SecurityQuestion;

public class DefaultSecurityQuestions {
	final public static String				SECURITY_QUESTION_DEFAULTS_FILENAME	= "security-question-defaults.yml";

	private static Map<String, String>	map;

	private DefaultSecurityQuestions() {
	}

	static {
		InputStream is = DefaultSecurityQuestions.class.getResourceAsStream(SECURITY_QUESTION_DEFAULTS_FILENAME);
		if (is == null) {
			is = DefaultSecurityQuestions.class.getClassLoader().getResourceAsStream(SECURITY_QUESTION_DEFAULTS_FILENAME);
		}
		String defaultsFileContents;
		try {
			defaultsFileContents = IOUtils.toString(is);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read security question defaults resource", e);
		}

		map = new HashMap<String,String>();
		Yaml yaml = new Yaml();
		Object loaded = yaml.load(defaultsFileContents);
		if (loaded instanceof Map) {
			Map<?, ?> questionMap = (Map<?, ?>) loaded;
			for(Object questionCode : questionMap.keySet()) {
				Object questionText = questionMap.get(questionCode);
				if(questionCode instanceof String && questionText instanceof String) {
					map.put(questionCode.toString(),questionText.toString());
				} else {
					throw new RuntimeException("Invalid entry in users defaults");
				}
			}
		} else {
			throw new RuntimeException("Invalid file format in users defaults");
		}
	}
	
	public static void sync() {
		Session session = ManagerPersistenceService.getInstance().getSessionWithTransaction();
		boolean completed = false;
		try {
			for (String questionCode : map.keySet()) {
				syncQuestion(session, questionCode, map.get(questionCode));
			}
			completed = true;
		} finally {
			if (completed)
				ManagerPersistenceService.getInstance().commitTransaction();
			else
				ManagerPersistenceService.getInstance().rollbackTransaction();
		}
	}

	private static void syncQuestion(Session session, String questionCode, String questionText) {
		SecurityQuestion question = (SecurityQuestion) session.bySimpleNaturalId(SecurityQuestion.class).load(questionCode);
		if(question == null) {
			question = new SecurityQuestion();
			question.setCode(questionCode);
		}
		question.setQuestion(questionText);
		session.saveOrUpdate(question);
	}
}
