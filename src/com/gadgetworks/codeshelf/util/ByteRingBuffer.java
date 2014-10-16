package com.gadgetworks.codeshelf.util;

import java.io.IOException;
import java.io.InputStream;

public class ByteRingBuffer extends InputStream {
	private byte[] buffer;
	
	int head, tail;
	
	public ByteRingBuffer(int length) {
		buffer = new byte[length];
		
		// head = 0; doesn't matter
		tail = -1;
	}
	
	public int length() {
		return buffer.length;
	}
	
	public int free() {
		if(tail < 0) {
			return buffer.length; // empty
		} //else
		if(head > tail) {
			return (buffer.length - head) + tail; // free at top plus free at bottom
		} //else
		return tail - head;
	}
	
	@Override
	public int available() { // available to be read
		if(tail < 0) {
			return 0; // empty
		} //else
		if(head > tail) {
			return head-tail;
		} //else
		return buffer.length - (tail - head); // length minus available
	}
	
	@Override
	public long skip(long n) {
		long skipped=0;
		while(n > 0) {
			n--;
			try {
				if(read() < 0) {
					n=0;
				} else {
					skipped++;
				}
			} catch (IOException e) {
				n=0;
			}
		}
		return skipped;
	}

	private void putWithoutChecking(byte b) {
		buffer[head] = b;
		head = (head+1) % buffer.length;
	}
	
	public void put(byte b) {
		if(free() == 0) {
			throw new IndexOutOfBoundsException("CircularByteBuffer overflow");
		}
		if(tail == -1) {
			head = 0;
			tail = 0;
		}
		putWithoutChecking(b);
	}
	
	public void put(byte[] buf) {
		if(free() < buf.length) {
			throw new IndexOutOfBoundsException("CircularByteBuffer overflow");
		}
		if(tail == -1) {
			head = 0;
			tail = 0;
		}
		for(int ix=0; ix<buf.length; ix++) {
			putWithoutChecking(buf[ix]);
		}
	}
	
	@Override
	public int read() throws IOException {
		if(available() == 0) {
			return -1;
		}// else
		
		int at = tail;
		tail = (tail+1) % buffer.length;
		if(tail == head) {
			// empty buffer, reset
			tail=-1;
			head=0;
		}		
		return (buffer[at] & 0xff);
	}
	
	public byte get() throws IOException {
		int result = read();
		if(result<0) {
			throw new IndexOutOfBoundsException("CircularByteBuffer underflow");
		}
		return (byte)result;
	}
}
