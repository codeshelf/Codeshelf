/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWsReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

/**
 * @author jeffw
 *
 */
public interface IWsReqCmd extends IWebSessionCmd {

	String	OP_TYPE				= "op";
	String	OP_TYPE_CREATE		= "cre";
	String	OP_TYPE_UPDATE		= "upd";
	String	OP_TYPE_DELETE		= "del";

	String	LAUNCH_CODE			= "LAUNCH_CODE";

	String	SUCCEED				= "SUCCEED";
	String	FAIL				= "FAIL";
	String	NEED_LOGIN			= "NEED_LOGIN";

	String	PERSISTENT_ID		= "persistentId";
	String	CLASSNAME			= "className";
	String	SHORT_DOMAIN_ID		= "domainId";
	String	DESC				= "description";
	String	METHODNAME			= "methodName";
	String	METHODARGS			= "methodArgs";
	String	DEVICE_GUID			= "deviceGuidStr";

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

	String	RESULTS				= "results";

	WsReqCmdEnum getCommandEnum();

	String getCommandId();

	void setCommandId(String inCommandId);

	// --------------------------------------------------------------------------
	/**
	 * Calling this method causes the session to execute the command and create the (optional) result.
	 * @return
	 */
	IWsRespCmd exec();

}
