package com.gadgetworks.codeshelf.ws.jetty;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonAutoDetect(getterVisibility=Visibility.NONE)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="className")
public class JsonPojo extends Object {

	@Getter @Setter
	String privateValue="privateValue";

	@JsonProperty
	String publicValue="publicValue";
}
