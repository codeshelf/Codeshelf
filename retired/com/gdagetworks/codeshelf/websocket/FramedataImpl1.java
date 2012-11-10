package com.gadgetworks.codeshelf.web.websocket;

import java.nio.ByteBuffer;

import com.gadgetworks.codeshelf.web.websocket.exceptions.InvalidFrameException;

public class FramedataImpl1 implements FrameBuilder {
	protected static byte[]	emptyarray	= {};
	protected boolean		fin;
	protected Opcode		optcode;
	protected ByteBuffer	unmaskedpayload;
	protected boolean		transferemasked;

	public FramedataImpl1() {
	}

	public FramedataImpl1(Opcode op) {
		this.optcode = op;
		unmaskedpayload = ByteBuffer.wrap(emptyarray);
	}

	public FramedataImpl1(Framedata f) {
		fin = f.isFin();
		optcode = f.getOpcode();
		unmaskedpayload = ByteBuffer.wrap(f.getPayloadData());
		transferemasked = f.getTransfereMasked();
	}

	@Override
	public boolean isFin() {
		return fin;
	}

	@Override
	public Opcode getOpcode() {
		return optcode;
	}

	@Override
	public boolean getTransfereMasked() {
		return transferemasked;
	}

	@Override
	public byte[] getPayloadData() {
		return unmaskedpayload.array();
	}

	@Override
	public void setFin(boolean fin) {
		this.fin = fin;
	}

	@Override
	public void setOptcode(Opcode optcode) {
		this.optcode = optcode;
	}

	@Override
	public void setPayload(byte[] payload) {
		unmaskedpayload = ByteBuffer.wrap(payload);
	}

	@Override
	public void setTransferemasked(boolean transferemasked) {
		this.transferemasked = transferemasked;
	}

	@Override
	public void append(Framedata nextframe) throws InvalidFrameException {
		if (unmaskedpayload == null) {
			unmaskedpayload = ByteBuffer.wrap(nextframe.getPayloadData());
		} else {
			// TODO might be inefficient. Cosider a global buffer pool
			ByteBuffer tmp = ByteBuffer.allocate(nextframe.getPayloadData().length + unmaskedpayload.capacity());
			tmp.put(unmaskedpayload.array());
			tmp.put(nextframe.getPayloadData());
		}
	}

	@Override
	public String toString() {
		return "Framedata{ optcode:" + getOpcode() + ", fin:" + isFin() + ", masked:" + getTransfereMasked() + ", payloadlength:" + unmaskedpayload.limit() + ", payload:"
				+ Charsetfunctions.utf8Bytes(new String(unmaskedpayload.array())) + "}";
	}

}
