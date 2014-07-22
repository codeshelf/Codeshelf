package com.gadgetworks.codeshelf.ws.jetty.client;

import java.util.LinkedList;
import java.util.Queue;

import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;

public class MessageQueue {
	
	Queue<MessageABC> queue = new LinkedList<MessageABC>();
	int maxQueueLength = 1000;
	
	public MessageQueue() {
	}

	public long getQueueLength() {
		return queue.size();
	}
	
	public synchronized boolean addMessage(MessageABC message) {
		if (queue.size()>=maxQueueLength) {
			// queue capacity exceeded
			return false;
		}
		queue.add(message);
		return true;
	}
	
	public MessageABC peek() {
		return queue.peek();
	}
	
	public boolean remove(MessageABC message) {
		return queue.remove(message);
	}
}
