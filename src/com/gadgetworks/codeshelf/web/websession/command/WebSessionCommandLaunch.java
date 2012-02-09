/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandLaunch.java,v 1.3 2012/02/09 07:29:23 jeffw Exp $
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
public class WebSessionCommandLaunch implements IWebSessionCommand {

	private static final String	LAUNCH_CODE	= "LAUNCH_CODE";

	private String 				mCommandId;
	private String				mLaunchCode;

	public WebSessionCommandLaunch(final String inCommandId, final JsonNode inDetailsAsJson) {
		mCommandId = inCommandId;
		JsonNode launchNode = inDetailsAsJson.get("launchCode");
		mLaunchCode = launchNode.getTextValue();
	}

	public final WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.LAUNCH_CODE;
	}

	public final String exec() {
		String result = null;

		try {
			StringWriter writer = new StringWriter();
			JsonFactory factory = new JsonFactory();
			JsonGenerator generator = factory.createJsonGenerator(writer);
			generator.writeStartObject();
			generator.writeStringField("id", mCommandId);
			generator.writeStringField("type", LAUNCH_CODE);
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

		//IWebSessionCommand.FAIL;

		// Search for a user with the specified ID (that has no password).
		User user = User.DAO.findById(mLaunchCode);

		// CRITICAL SECURITY CONCEPT.
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).
		if ((user != null) && (user.getActive())) {
			if (user.getHashedPassword() != null) {
				//result = IWebSessionCommand.NEED_LOGIN;
			} else {
				//result = IWebSessionCommand.SUCCEED;
			}
		}

		return result;
	}
}
