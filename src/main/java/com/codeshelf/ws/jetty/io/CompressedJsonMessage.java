package com.codeshelf.ws.jetty.io;

import java.nio.charset.Charset;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* compressed message adds 6 byte header:
 * 
 * byte 0 = !
 * byte 1 = 0x0 (reserved)
 * byte 2-5 = message size (most significant byte first)
 * 
 * */

public class CompressedJsonMessage {
	public final static byte compressedMessageHeaderStart = '!';
	public static final int JSON_COMPRESS_MAXIMUM = 0x7E000000; // LZ4 library maximum

	private static final Logger	LOGGER = LoggerFactory.getLogger(CompressedJsonMessage.class);
	private static final LZ4Factory lz4Factory = LZ4Factory.safeInstance();

	private static final Charset clean8BitCharset = Charset.forName("ISO-8859-1");
	private static final Charset internationalCharset = Charset.forName("UTF-8");
	
	private final static byte reserved = 0;
	
	int uncompressedLength;
	String compressed;
	String uncompressed;

	public CompressedJsonMessage(String json,boolean inputCompressed) {
		if(inputCompressed) {
			compressed=json;
			decompress();
		} else {
			uncompressed=json;
			compress();
		}
	}
	
	public int getHeaderLength() {
		return 6;
	}
	
	public int getUncompressedLength() {
		return uncompressedLength;
	}
	
	public int getCompressedLength() {
		return compressed.length();
	}
	
	public String getCompressed() {
		return compressed;
	}
	
	public String getUncompressed() {
		return uncompressed;
	}
	
	private void compress() {
		LZ4Compressor compressor=lz4Factory.fastCompressor();
		
		// first convert string 
		byte[] uncompressedBytes=null;
		uncompressedBytes = uncompressed.getBytes(internationalCharset);
		
		uncompressedLength = uncompressedBytes.length;
		// create buffer with header and room for compressed data (note this is larger than input!)
		byte[] buffer = new byte[compressor.maxCompressedLength(uncompressedBytes.length ) + this.getHeaderLength()];
		buffer[0] = CompressedJsonMessage.compressedMessageHeaderStart;
		buffer[1] = reserved;
		buffer[2] = (byte) (uncompressedBytes.length >> 24);
		buffer[3] = (byte)((uncompressedBytes.length >> 16) & 0xff);
		buffer[4] = (byte)((uncompressedBytes.length >> 8) & 0xff);
		buffer[5] = (byte) (uncompressedBytes.length  & 0xff);

		// now compress into buffer
		int compressedLength = compressor.compress(uncompressedBytes,0,uncompressedBytes.length,buffer,this.getHeaderLength());
		//LOGGER.debug("Compressing "+this.getUncompressedLength()+" bytes to "+compressedLength+" (total buffer "+buffer.length+")");

		// store result in String using 8 bit clean charset
		this.compressed = new String(buffer, 0, compressedLength+this.getHeaderLength(), clean8BitCharset);
	}
	
	private byte[] readHeader() {
		if(compressed.charAt(0)!=CompressedJsonMessage.compressedMessageHeaderStart) {
			return null;
		}

		byte[] buffer;
		buffer = compressed.getBytes(clean8BitCharset);

		if(buffer[0] != CompressedJsonMessage.compressedMessageHeaderStart) {
			return null;
		}
		if(buffer[1] != 0) {
			return null;
		}
		int length = ((buffer[2] & 0xff) << 24) 
					+((buffer[3] & 0xff) << 16)
					+((buffer[4] & 0xff) << 8)
					+ (buffer[5] & 0xff);
		if ((length<0) || (length>CompressedJsonMessage.JSON_COMPRESS_MAXIMUM)) {
			return null;
		}
		//else
		uncompressedLength = length;
		return buffer;
	}
	
	private void decompress() {
		byte[] rawBytes = readHeader();
		if(rawBytes == null) {
			uncompressed = compressed; // if no header, pass through input
		} else {
			// allocate buffer for uncompressed data (known size)
			byte[] decompressedBytes = new byte[this.getUncompressedLength()];
			// decompress
			//LOGGER.debug("Decompressing "+(rawBytes.length-this.getHeaderLength())+" bytes to "+this.getUncompressedLength()+" (total buffer "+rawBytes.length+")");
			int inputRead = lz4Factory.fastDecompressor().decompress(rawBytes,this.getHeaderLength(),decompressedBytes,0,this.getUncompressedLength());
			
			int expectedInputRead = rawBytes.length - this.getHeaderLength();
			if(inputRead != expectedInputRead) {
				LOGGER.error("Input read length did not match, expected "+expectedInputRead+" got "+inputRead);
			}
			
			// next, convert back to international character set
			uncompressed = new String(decompressedBytes,internationalCharset);
		}
	}

}