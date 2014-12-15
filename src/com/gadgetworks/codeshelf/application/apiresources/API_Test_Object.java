package com.gadgetworks.codeshelf.application.apiresources;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class API_Test_Object {
	private String name = "Ilya";
	public int age = 26;
	@JsonProperty
	private int x = 123;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
