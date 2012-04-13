/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionReqCmd.java,v 1.6 2012/04/13 18:54:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;

/**
 * @author jeffw
 *
 */
public interface IWebSessionReqCmd extends IWebSessionCmd {

	String	OP_TYPE				= "opType";
	String	OP_TYPE_CREATE		= "create";
	String	OP_TYPE_UPDATE		= "update";
	String	OP_TYPE_DELETE		= "delete";

	String	LAUNCH_CODE			= "LAUNCH_CODE";

	String	SUCCEED				= "SUCCEED";
	String	FAIL				= "FAIL";
	String	NEED_LOGIN			= "NEED_LOGIN";

	String	PERSISTENT_ID		= "persistentId";
	String	CLASSNAME			= "className";

	String	PARENT_CLASS		= "parentClassName";
	String	PARENT_ID			= "parentPersistentId";

	String	GETTER_METHOD		= "getterMethod";
	String	SETTER_METHOD		= "setterMethod";
	String	SETTER_VALUE		= "setterValue";

	String	OBJECT_ID_LIST		= "objectIds";

	String	PROPERTY_NAME_LIST	= "propertyNames";
	String	PROPERTIES			= "properties";
	String	FILTER_CLAUSE		= "filterClause";
	String	FILTER_PARAMS		= "filterParams";

	String	RESULTS				= "result";

	WebSessionReqCmdEnum getCommandEnum();

	String getCommandId();

	void setCommandId(String inCommandId);

	// --------------------------------------------------------------------------
	/**
	 * Calling this method causes the session to execute the command and create the (optional) result.
	 * @return
	 */
	IWebSessionRespCmd exec();

}
