package com.gadgetworks.codeshelf.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TimeZone;

public class PcapRingBuffer {
	static public final int DEFAULT_SIZE = 1024*100; // 100kB
	static public final int DEFAULT_SLACK = 1024; // 1% slack

	private ByteRingBuffer byteRing;
	private int targetSlackBytes;
	private int available;
	private int datalinkId;
	
	public PcapRingBuffer(int lengthBytes, int targetAvailableBytes, int datalinkId) {
		this.available = 0;
		this.byteRing = new ByteRingBuffer(lengthBytes);
		this.targetSlackBytes = targetAvailableBytes;
		this.datalinkId = datalinkId;
	}
	
	public void put(PcapRecord record) throws IOException {
		int spaceWanted = record.getLength();

		synchronized (this) {
			if(spaceWanted > byteRing.length()) {
				throw new IndexOutOfBoundsException("Packet larger than buffer ("+spaceWanted+" > "+byteRing.length()+")");
			}
			// attempt to reserve extra room, unless hold is on or packet is too big
			if (/*!hold && */(spaceWanted + this.targetSlackBytes <= byteRing.length())) {
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
					// keep waiting?
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

	public byte[] bytes() {
		byte[] result;
		synchronized(this) {
			result = this.byteRing.bytes();
		}
		return result;
	}
	
	public PcapRecord[] records() throws IOException {
		PcapRecord[] result;
		synchronized(this) {
			// TODO: determine whether this operation takes too long to block. 
			// probably okay, but maybe caller needs to clone ring instead?
			this.byteRing.freeze();
			result=new PcapRecord[available];
			for(int ix=0;ix<available;ix++) {
				result[ix]=new PcapRecord(byteRing);
			}
			this.byteRing.unfreeze(true);
		}
		return result;
	}

	public int availableRecords() {
		return this.available;
	}
	
	public int availableBytes() {
		return this.byteRing.available();
	}
	
	private boolean unsafeCheckAvailable() {
		return this.available > 0;
	}

	public byte[] generatePcapHeader() {
		/*
		typedef struct pcap_hdr_s {
		        guint32 magic_number;   
		        guint16 version_major;  
		        guint16 version_minor;  
		        gint32  thiszone;       
		        guint32 sigfigs;        
		        guint32 snaplen;        
		        guint32 network;        
		} pcap_hdr_t;
		 */
		ByteBuffer bb = ByteBuffer.allocate(24); // 24 bytes
		bb.order(ByteOrder.nativeOrder());
		bb.clear();
		
		bb.putInt(0x1b2c3d4); // magic_number
		bb.putShort((short)2); // major version number
		bb.putShort((short)4); // minor version number
		bb.putInt(TimeZone.getDefault().getRawOffset() / 1000); // timezone in seconds
		bb.putInt(0); // accuracy of timestamps
		bb.putInt(PcapRecord.PACKET_TRUNCATE_LENGTH); // snaplen
		bb.putInt(datalinkId); // data link type		
		bb.flip();
		
		byte[] result=new byte[24];
		bb.get(result,0,24);
		return result;
	}

}
