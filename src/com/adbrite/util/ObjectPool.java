package com.adbrite.util;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class ObjectPool<O> {
	private final LinkedBlockingQueue<O> q = new LinkedBlockingQueue<O>();
	
	private int highWaterMark = 10;
	
	public int getPoolSize() {
		return q.size();
	}
	/**
	 * Borrow object from the pool. RuntimeException could be thrown if the object
	 * could not be created.
	 * 
	 * @return the object
	 */
	public O borrowObject() {
		O obj = q.poll();
		if(obj!=null)
			return obj;
		return createObject();
	}
	
	public void returnObject(O obj) {
		if(getPoolSize()<highWaterMark && validateObject(obj))
			q.offer(obj);
		else
			discardObject(obj);
	}

	/**
	 * Create the new object. should throw RuntimeException if the object could not be created
	 * @return
	 */
	abstract protected O createObject();
	
	/**
	 * Validate object before returning to the pool
	 * @param obj
	 * @return true if object is good, false if object should be discarded
	 */
	protected boolean validateObject(O obj) {
		return true;
	}

	/**
	 * de-initialize the object. good place to close sockets, files etc.
	 * @param obj
	 */
	protected void discardObject(O obj) {
	}
	
	public void close() {
		ArrayList<O> items = new ArrayList<O>(q.size());
		q.drainTo(items);
		for (O o : items) {
			discardObject(o);
		}
		
	}
	public int getHighWaterMark() {
		return highWaterMark;
	}
	public void setHighWaterMark(int highWaterMark) {
		this.highWaterMark = highWaterMark;
	}
}
