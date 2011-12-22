package com.adbrite.netty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NetworkOperation<TSEND, TRECEIVE> implements Operation, TimerTask {
	private static final boolean DEBUG_CALLPATH = false;
	private volatile IllegalStateException prevCallStack;
	
	private static Logger LOG = LoggerFactory.getLogger(NetworkOperation.class);

	private static final Timer timer = NettyPersistentConnection.timer;
	private static final int DEFAULT_RETRY_LIMIT = 3;

	private boolean finished = false;
	private volatile Throwable failureReason;
	private final Timeout timeout;
	private final long startTimeNs = System.nanoTime();
	private final int retryLimit;
	private final AtomicInteger retryCount = new AtomicInteger(0);
	volatile Channel channel; //set the channel this op is executing on
	private volatile OpExecutionStats statsCallback;

	/**
	 * Get time elapsed since op start in nanoseconds
	 * @return time elapsed since op start in nanoseconds
	 */
	public long getExecutionTime() {
		long endTime = System.nanoTime();
		return endTime-startTimeNs;
	}
	public NetworkOperation(int timeoutMs) {
		this(timeoutMs, DEFAULT_RETRY_LIMIT);
	}
	
	public NetworkOperation(int timeoutMs, int retryLimit) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("creating NetworkOperation {}, timeout "+timeoutMs, this.getClass().getName());
		}
		timeout = timer.newTimeout(this, timeoutMs, TimeUnit.MILLISECONDS);
		this.retryLimit = retryLimit;
	}
	
	public int getRetryLimit() {
		return retryLimit;
	}
	
	public int getRetryCount() {
		return retryCount.get();
	}

	/**
	 * Increment retry counter
	 * @return true if should retry
	 */
	public boolean shouldRetry() {
		return (retryCount.incrementAndGet()<=retryLimit);
	}
	/**
	 * TimerTask interface, NEVER call it
	 * @param timeout
	 * @throws Exception
	 */
    public void run(Timeout timeout) throws Exception {
		if(LOG.isDebugEnabled()) {
			String className = this.getClass().getSimpleName();
			if(className==null)
				className = this.getClass().getName();
			LOG.debug("NetworkOperation {} timeout fired after "+TimeUnit.NANOSECONDS.toMillis(getExecutionTime())+" ms", className);
		}
    	notifyListener(new TimeoutException("Network OP timeout expired"));
    }

    /**
     * Notify listener about failure
     * @param failureReason
     */
	public void notifyListener(Throwable failureReason) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("NetworkOperation {} failed after "+TimeUnit.NANOSECONDS.toMillis(getExecutionTime())+"ms", this.getClass().getName(), failureReason);
		}
		synchronized (this) {
			if(finished) {
				if(LOG.isDebugEnabled())
					LOG.debug("notifyListener() called on finished NetworkOperation");
				if(DEBUG_CALLPATH) {
					LOG.error("Current call stack", new IllegalStateException());
					LOG.error("Previous call stack", prevCallStack);
				}
				return;
			}
			finished = true;
			if(DEBUG_CALLPATH)
				prevCallStack = new IllegalStateException("Originall call stack");
			timeout.cancel();
		}
		if(channel!=null) {
			channel.close();
			channel=null;
		}
		this.failureReason = failureReason;
		if(statsCallback!=null)
			statsCallback.registerFailure(getExecutionTime(), failureReason);
		try {
			operationCompleted();
		} catch (Throwable e) {
			LOG.error("IGNORING Exception in operationCompleted()", e);
		}
	}
	
	/**
	 * Call the callback once 
	 */
	public void notifyListener() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("NetworkOperation {} completed after "+TimeUnit.NANOSECONDS.toMillis(getExecutionTime())+" ms", this.getClass().getName());
		}
		synchronized (this) {
			if(finished) {
				if(LOG.isDebugEnabled())
					LOG.debug("notifyListener() called on finished NetworkOperation");
				if(DEBUG_CALLPATH) {
					LOG.error("Current call stack", new IllegalStateException());
					LOG.error("Previous call stack", prevCallStack);
				}
				return;
			}
			finished = true;
			if(DEBUG_CALLPATH)
				prevCallStack = new IllegalStateException("Originall call stack");
			timeout.cancel();
		}
		channel=null;
		if(statsCallback!=null)
			statsCallback.registerSuccess(getExecutionTime());
		try {
			operationCompleted();
		} catch (Throwable e) {
			LOG.error("IGNORING Exception in operationCompleted()", e);
		}
	}
	
	public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	/**
	 * This operation is finished - failed or succeded
	 * @return
	 */
	public boolean isFinished() {
		synchronized (this) {
			return finished;
		}
	}

	public Throwable getFailureReason() {
		return failureReason;
	}

	public final boolean isSucess() {
		return failureReason==null;
	}

	/**
	 * Build and return a request buffer (for raw channel) or object for sending to pipe
	 * Could be called several times, the operation object MUST RESET its state
	 * and return the request buffer.
	 * @return request buffer with trailing \r\n
	 */
	protected abstract TSEND resetAndBuildRequest();
	/**
	 * read (potentially partial or excessive) packet, decode, set success flag
	 * remove all consumed bytes or they will be passed again
	 * do not call the completion callback!
	 * @return true if done decoding 
	 * @throws ProtocolException 
	 */
	protected abstract boolean readFromBuffer(TRECEIVE data) throws ProtocolException;
	public OpExecutionStats getStatsCallback() {
		return statsCallback;
	}
	public void setStatsCallback(OpExecutionStats statsCallback) {
		this.statsCallback = statsCallback;
	}

}
