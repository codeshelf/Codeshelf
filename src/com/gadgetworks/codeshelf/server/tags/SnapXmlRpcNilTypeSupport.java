/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SnapXmlRpcNilTypeSupport.java,v 1.2 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.NullParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.NullSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.xml.sax.SAXException;

public class SnapXmlRpcNilTypeSupport extends TypeFactoryImpl {

	public SnapXmlRpcNilTypeSupport(XmlRpcController inController) {
		super(inController);
	}

	public TypeParser getParser(XmlRpcStreamConfig inConfig, NamespaceContextImpl inContext, String inUrl, String inLocalName) {
		if (NullSerializer.NIL_TAG.equals(inLocalName)) {
			return new NullParser();
		} else {
			return super.getParser(inConfig, inContext, inUrl, inLocalName);
		}
	}

	public TypeSerializer getSerializer(XmlRpcStreamConfig inConfig, Object inObject) throws SAXException {
		if (inObject instanceof SnapXmlRpcNilTypeSupport) {
			return new NullSerializer();
		} else {
			return super.getSerializer(inConfig, inObject);
		}
	}
}
