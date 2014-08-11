package com.gadgetworks.codeshelf.filter;

public enum EventType {

	Create("cre"),
	Update("upd"),
	Delete("del");
	
	private String name;
	
	private EventType(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
