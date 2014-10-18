package com.gadgetworks.codeshelf.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonIgnore;

/*
typedef struct pcaprec_hdr_s {
        guint32 ts_sec;
        guint32 ts_usec;
        guint32 incl_len;
        guint32 orig_len;
} pcaprec_hdr_t;
 */

public class PcapRecord {
	final static public int HEADER_LENGTH = 16;  
	final static public int PACKET_TRUNCATE_LENGTH = 65535; 
	
    final public static int LINKTYPE_DLT_USER0 = 147;
    final public static int getLinkTypeId() { return LINKTYPE_DLT_USER0; } 
    
    final public static byte[] prepend = {(byte)(0x7e), (byte)(0xff)};

	@Getter
    private long microseconds;         /* timestamp microseconds (including whole seconds) */        
	@Getter
    private int originalLength;       /* actual length of packet */
	@Getter
	private byte[] packet;
	
	public PcapRecord(byte[] data) {
		originalLength = data.length + prepend.length;
		if(originalLength > PcapRecord.PACKET_TRUNCATE_LENGTH) {
			packet = Arrays.copyOf(data, PcapRecord.PACKET_TRUNCATE_LENGTH - prepend.length);
		} else {
			packet = data;
		}
		microseconds = 1000 * System.currentTimeMillis();
	}
	
	public PcapRecord(InputStream raw) throws IOException {
		this.microseconds = 1000000 * readUint32(raw);
		this.microseconds += readUint32(raw);
		int expectedPacketLength = readInt32(raw) - prepend.length;
		this.originalLength = readInt32(raw);
		for(int i=0; i< prepend.length; i++) {
			if(prepend[i] != (byte)raw.read()) {
				throw new IOException("corrupt buffer (prepend bytes didn't match)");
			}
		}
		this.packet = new byte[expectedPacketLength];
		int actualSize = raw.read(this.packet);
		if(actualSize != expectedPacketLength) {
			throw new IOException("unexpected end of buffer while reconstructing packet");
		}
	}

	private long readUint32(InputStream raw) throws IOException {
		long result = ((raw.read() & 0xff) << 24);
		result += ((raw.read() & 0xff) << 16);
		result += ((raw.read() & 0xff) << 8);
		result += (raw.read() & 0xff);
		return result;
	}
	
	private int readInt32(InputStream raw) throws IOException {
		return (int)readUint32(raw);
	}
	
	@JsonIgnore
	public byte[] getHeaderAndPrependBytes() {
		byte[] result = new byte[PcapRecord.HEADER_LENGTH + PcapRecord.prepend.length];
		long ts_sec = (this.microseconds/1000000);
		long ts_usec = (this.microseconds%1000000);
		
		// always big-endian
		
		result[0] = (byte)(ts_sec >> 24);
		result[1] = (byte)(ts_sec >> 16);
		result[2] = (byte)(ts_sec >> 8);
		result[3] = (byte)(ts_sec);
		
		result[4] = (byte)(ts_usec >> 24);
		result[5] = (byte)(ts_usec >> 16);
		result[6] = (byte)(ts_usec >> 8);
		result[7] = (byte)(ts_usec);
		
		result[8] = (byte)((packet.length+prepend.length) >> 24);
		result[9] = (byte)((packet.length+prepend.length) >> 16);
		result[10] = (byte)((packet.length+prepend.length) >> 8);
		result[11] = (byte)((packet.length+prepend.length));
		
		result[12] = (byte)(originalLength >> 24);
		result[13] = (byte)(originalLength >> 16);
		result[14] = (byte)(originalLength >> 8);
		result[15] = (byte)(originalLength);
		
		for(int i=0; i<PcapRecord.prepend.length; i++) {
			result[16+i] = prepend[i];
		}
		return result;
	}
	
	@JsonIgnore
	public int getLength() {
		return PcapRecord.HEADER_LENGTH + this.packet.length + PcapRecord.prepend.length;
	}
	
	@JsonIgnore
	public byte getSourceAddress() {
		if(this.packet.length > 4) {
			return packet[1];
		}//else
		return -1;
	}

	@JsonIgnore
	public byte getDestinationAddress() {
		if(this.packet.length > 4) {
			return packet[2];
		}//else
		return -1;
	}

	@Override
	public String toString() {
		String result = String.format("[%4d] %s%s", 
							PcapRecord.prepend.length + this.packet.length,
							//this.originalLength,
							StringUIConverter.bytesToHexString(PcapRecord.prepend),
							StringUIConverter.bytesToHexString(this.getPacket())
						);
		return result;
	}
}
