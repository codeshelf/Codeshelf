package com.codeshelf.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ForwardingBlockingQueue;

//TODO implement evicting
public class EvictingBlockingQueue<E> extends ForwardingBlockingQueue<E> {

	private final BlockingQueue<E> delegate;
	
	public EvictingBlockingQueue(int capacity) {
		this.delegate = new ArrayBlockingQueue<E>(capacity);
	}
	
	@Override
	protected BlockingQueue<E> delegate() {
		return delegate;
	}
	
	  /**
	   * Adds the given element to this queue. If the queue is currently full, the element at the head
	   * of the queue is evicted to make room.
	   *
	   * @return {@code true} always
	   */
	  @Override public boolean offer(E e) {
	    return add(e);
	  }

	  /**
	   * Adds the given element to this queue. If the queue is currently full, the element at the head
	   * of the queue is evicted to make room.
	   *
	   * @return {@code true} always
	   */
	  @Override public boolean add(E e) {
	    Preconditions.checkNotNull(e);  // check before removing
	    if (delegate().remainingCapacity() == 0) {
		      delegate.remove();
	    }
	    delegate.add(e);
	    return true;
	  }
}