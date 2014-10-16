package com.gadgetworks.codeshelf.util;

import java.io.IOException;

public class PcapRingBuffer {
	static public final int DEFAULT_SIZE = 1024*100; // 100kB
	static public final int DEFAULT_SLACK = 1024; // 1% slack

	private ByteRingBuffer byteRing;
	private int targetSlackBytes;
	private int available;
	private boolean hold;
	
	public PcapRingBuffer(int lengthBytes, int targetAvailableBytes) {
		this.hold = false;
		this.available = 0;
		this.byteRing = new ByteRingBuffer(lengthBytes);
		this.targetSlackBytes = targetAvailableBytes;
	}
	
	public void put(PcapRecord record) throws IOException {
		int spaceWanted = record.getLength();

		synchronized (this) {
			if(spaceWanted > byteRing.length()) {
				throw new IndexOutOfBoundsException("Packet larger than buffer ("+spaceWanted+" > "+byteRing.length()+")");
			}
			// attempt to reserve extra room, unless hold is on or packet is too big
			if (!hold && (spaceWanted + this.targetSlackBytes <= byteRing.length())) {
				spaceWanted += this.targetSlackBytes;
			}
			// make room
			while(byteRing.free() < spaceWanted) {
				if(this.get() == null) {
					throw new RuntimeException("Internal error: could not free space on packet ring buffer");
				}
			}
			byteRing.put(record.getHeaderBytes());
			byteRing.put(record.getPacket());
			this.available++;
			this.notify();
		}
	}
	
	public PcapRecord blockingGet() throws IOException {
		PcapRecord result = null;
		synchronized(this) { 
			while(!unsafeCheckAvailable()) {
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
			result = new PcapRecord(this.byteRing);
			this.available--;
		}
		return result;
	}
	
	public PcapRecord get() throws IOException {
		PcapRecord result = null;		
		synchronized(this) {
			if(unsafeCheckAvailable()) {
				result = new PcapRecord(this.byteRing);
				if(result != null) {
					this.available--;
				}
			}
		}
		return result;
	}
	
	public int available() {
		return this.available;
	}
	
	private boolean unsafeCheckAvailable() {
		return this.available > 0;
	}
}
