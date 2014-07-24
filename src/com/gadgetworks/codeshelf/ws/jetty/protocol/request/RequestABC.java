package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import org.atteo.classindex.IndexSubclasses;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

//@IndexSubclasses
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
public abstract class RequestABC extends MessageABC {
}
