package com.codeshelf.flyweight.controller;

public interface PacketCaptureListener {
	void capture(byte[] packet);
}
