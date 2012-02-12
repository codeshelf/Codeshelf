/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandLaunch.java,v 1.4 2012/02/12 02:08:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import java.io.IOException;
import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.model.persist.User;

/**
 * @author jeffw
 *
 */
public class WebSessionCommandLaunch extends WebSessionCommandABC {

	private static final String	LAUNCH_CODE	= "LAUNCH_CODE";

	private String				mLaunchCode;

	public WebSessionCommandLaunch(final JsonNode inCommandAsJson) {
		super(inCommandAsJson);

		JsonNode dataJsonNode = getDataJsonNode();
		JsonNode launchNode = dataJsonNode.get("launchCode");
		mLaunchCode = launchNode.getTextValue();
	}

	public final WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.LAUNCH_CODE;
	}

	public final String exec() {
		String result = null;

		String authenticateResult = IWebSessionCommand.FAIL;

		// Search for a user with the specified ID (that has no password).
		User user = User.DAO.findById(mLaunchCode);

		// CRITICAL SECURITY CONCEPT.
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).
		if ((user != null) && (user.getActive())) {
			if (user.getHashedPassword() != null) {
				authenticateResult = IWebSessionCommand.NEED_LOGIN;
			} else {
				authenticateResult = IWebSessionCommand.SUCCEED;
			}
		}

		try {
			StringWriter writer = new StringWriter();
			JsonFactory factory = new JsonFactory();
			JsonGenerator generator = factory.createJsonGenerator(writer);
			generator.writeStartObject();
			generator.writeStringField("id", getCommandId());
			generator.writeStringField("type", LAUNCH_CODE);
			generator.writeObjectFieldStart("data");
			generator.writeStringField("LAUNCH_CODE_RESULT", authenticateResult);
			generator.writeEndObject();
			generator.writeEndObject();
			generator.close();
			result = writer.toString();
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
}
