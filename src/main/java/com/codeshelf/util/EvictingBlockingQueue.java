package com.codeshelf.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.common.util.concurrent.ForwardingBlockingQueue;

//TODO implement evicting
public class EvictingBlockingQueue<T> extends ForwardingBlockingQueue<T> {

	private final BlockingQueue<T> delegate;
	
	public EvictingBlockingQueue(int capacity) {
		this.delegate = new ArrayBlockingQueue<T>(capacity);
	}
	
	@Override
	protected BlockingQueue<T> delegate() {
		return delegate;
	}
}