package com.adbrite.netty;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counting barrier, use it when you need to asynchronously wait for completion of
 * several operations executed in parallel.
 * 
 * Interface if this barrier is error-prone, however it designed this way
 * to make it more efficient.
 * The only callback is object method so no additional callback objects
 * or lists created.
 * 
 * To use the barrier you need to 
 * create the barrier object.
 *  override operationCompleted
 *  override the init operation and initialize and run all operations in it.
 *  use raiseBarrir() to add operation, call lowerBarrir() on the object returned to 
 *  If result of operation require starting new operation 
 * 	a. call raiseBarrier() then 
 *  b. start the op
 *  c. call the lowerBarrier as a signal that original op is finished!
 * see {@link com.adbrite.netty.memcache.NettyMemcacheConnectionTest} for usage example.
 * 
 * To consider:
 * Callback (operationCompleted()) is always executed on the thread (executor) 
 * happened to be the last one calling lowerBarrier. If we need it to be executed
 * in the predefined Executor we better use executor as one of the parameters like
 * Google's com.google.common.util.concurrent.ListenableFuture does. 
 * 
 */
public abstract class OperationsBarrier implements Operation {
	/**
	 * We raise the barrier by obtaining a lease.
	 * the barrier is lowered to 0 when we have all leases released
	 * @author ap
	 *
	 */
	public class Lease {
		private final AtomicBoolean finished = new AtomicBoolean(false);
		/**
		 * This method should be called from operationCompleted() of all of dependent operations
		 */
		public void lowerBarrier() {
			if(!finished.compareAndSet(false, true)) {
				throw new IllegalStateException("this lease is already released");
			}
			decr();
		}
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(OperationsBarrier.class);
	private static final Timer timer = NettyPersistentConnection.timer;
	//TODO: implement fuse timer
	//private final Timeout timeout;
	private static long TIMEOUT_MS = 20000; //this is a fuse timeout.

	//operations could be completed in parallel in different threads
	private final AtomicInteger cnt;
	
	//private Operation listener=null;
	
	/**
	 * Override this method if you need to initialize some object fields.
	 */
	abstract protected void init();
	
	public OperationsBarrier() {
		cnt = new AtomicInteger(1);
		//timeout = timer.newTimeout(this, TIMEOUT_MS, TimeUnit.MILLISECONDS);
//		init();
//		start();
	}

	/**
	 * Add one operation to the barrier. the operation callback should call lowerBarrier()
	 * @return 
	 */
	public Lease raiseBarrier() {
		int i=cnt.incrementAndGet();
		if(LOG.isDebugEnabled())
			LOG.debug("in addOperation(), cnt = {}",i);
		return new Lease();
	}
	
	private synchronized void decr() {
		int i=cnt.decrementAndGet();
		if(LOG.isDebugEnabled())
			LOG.debug("in decr(), cnt = {}",i);
		assert(i>=0);
		if(i==0) {
			operationCompleted();
		}
	}

	public OperationsBarrier start() {
		init();
		decr();
		return this;
	}

}
