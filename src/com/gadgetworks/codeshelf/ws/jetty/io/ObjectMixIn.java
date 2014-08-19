package com.gadgetworks.codeshelf.ws.jetty.io;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="className")
public abstract class ObjectMixIn {
}
