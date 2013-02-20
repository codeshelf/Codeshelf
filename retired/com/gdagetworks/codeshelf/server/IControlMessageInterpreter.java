package com.gadgetworks.codeshelf.server;

public interface IControlMessageInterpreter extends WidgetCommandListener {
	
	void processControlMessage(ControlMessage inControlMessage);

}
