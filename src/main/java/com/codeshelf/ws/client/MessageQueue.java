package com.codeshelf.ws.client;

import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.ws.protocol.message.MessageABC;

public class MessageQueue {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(MessageQueue.class);

	Queue<MessageABC> queue = new LinkedList<MessageABC>();
	int maxQueueLength = 1000;
	
	public MessageQueue() {
	}

	public long getQueueLength() {
		return queue.size();
	}
	
	public synchronized boolean addMessage(MessageABC message) {
		if (queue.contains(message)) {
			LOGGER.warn("Failed to add message to queue: Message is already queued");
			return false;
		}
		if (queue.size()>=maxQueueLength) {
			// queue capacity exceeded
			return false;
		}
		return queue.add(message);
	}
	
	public synchronized MessageABC peek() {
		return queue.peek();
	}
	
	public synchronized boolean remove(MessageABC message) {
		return queue.remove(message);
	}
}
