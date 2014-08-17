package com.gadgetworks.codeshelf.ws.command.resp;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class InvalidWsRespCmd extends WsRespCmdABC {

	private final String mMessage;
	
	public InvalidWsRespCmd(String message) {
		super();
		mMessage = message;
	}
	
	@Override
	public WsRespCmdEnum getCommandEnum() {
		return 	WsRespCmdEnum.INVALID;
	}

	@Override
	protected void doPrepareDataNode(ObjectNode inOutDataNode) {
		// TODO Auto-generated method stub
		inOutDataNode.put("message", mMessage);
	}
}
