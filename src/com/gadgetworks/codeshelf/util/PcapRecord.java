package com.gadgetworks.codeshelf.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import lombok.Getter;

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
	final static public int MAX_PACKET_LENGTH = 65535; 

	@Getter
    private long microseconds;         /* timestamp microseconds (including whole seconds) */        
	@Getter
    private int storedLength;       /* number of octets of packet saved in file */
	@Getter
    private int originalLength;       /* actual length of packet */
	@Getter
	private byte[] packet;
	
	public PcapRecord(byte[] data) {
		originalLength = data.length;
		if(originalLength > PcapRecord.MAX_PACKET_LENGTH) {
			storedLength = PcapRecord.MAX_PACKET_LENGTH;
			packet = Arrays.copyOf(data, storedLength);
		} else {
			storedLength = originalLength;
			packet = data;
		}
		microseconds = 1000 * System.currentTimeMillis();
	}
	
	public PcapRecord(InputStream raw) throws IOException {
		this.microseconds = 1000000 * readUint32(raw);
		this.microseconds += readUint32(raw);
		this.storedLength = readInt32(raw);
		this.originalLength = readInt32(raw);
		this.packet = new byte[storedLength];
		int actualSize = raw.read(this.packet);
		if(actualSize != this.storedLength) {
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
	
	public byte[] getHeaderBytes() {
		byte[] result = new byte[PcapRecord.HEADER_LENGTH];
		long ts_sec = (this.microseconds/1000000);
		long ts_usec = (this.microseconds%1000000);
		result[0] = (byte)(ts_sec >> 24);
		result[1] = (byte)(ts_sec >> 16);
		result[2] = (byte)(ts_sec >> 8);
		result[3] = (byte)(ts_sec);
		
		result[4] = (byte)(ts_usec >> 24);
		result[5] = (byte)(ts_usec >> 16);
		result[6] = (byte)(ts_usec >> 8);
		result[7] = (byte)(ts_usec);
		
		result[8] = (byte)(storedLength >> 24);
		result[9] = (byte)(storedLength >> 16);
		result[10] = (byte)(storedLength >> 8);
		result[11] = (byte)(storedLength);
		
		result[12] = (byte)(originalLength >> 24);
		result[13] = (byte)(originalLength >> 16);
		result[14] = (byte)(originalLength >> 8);
		result[15] = (byte)(originalLength);
		return result;
	}
	
	public int getLength() {
		return PcapRecord.HEADER_LENGTH + this.packet.length;
	}
	
	@Override
	public String toString() {
		SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); // TODO: DUHHHHH
		
		String result = String.format("%s [%4d/%4d] %s", 
							TIMESTAMP_FORMAT.format(new Date(this.microseconds/1000)),
							this.storedLength,
							this.originalLength,
							StringUIConverter.bytesToHexString(this.getPacket())
						);
		return result;
	}
	
}
