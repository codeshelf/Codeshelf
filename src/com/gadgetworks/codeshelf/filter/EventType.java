package com.gadgetworks.codeshelf.filter;

public enum EventType {

	Create("cre"),
	Update("upd"),
	Delete("del");
	
	private String code;
	
	private EventType(String code) {
		this.code = code;
	}
	
	@Override
	public String toString() {
		return this.code;
	}
}
