package com.adbrite.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback for asynchronous service completion.
 * Using separate methods onComplete and onError causes exponential code
 * branching. Considering we are tolerating errors in most cases this approach
 * should work.
 * Implement your onError to override this behavior
 * 
 * @author apesternikov
 *
 * @param <T>
 */
public abstract class AsyncCallback<T> {
	private static final boolean DEBUG_CALLPATH = false;
	private static Logger LOG = LoggerFactory.getLogger(AsyncCallback.class);
	private boolean finished = false;
	
	private volatile IllegalStateException prevCallStack;
	
	public final void notifyListener(T response) {
		synchronized (this) {
			if(finished) {
				LOG.error("notifyListener(T response) called on finished AsyncCallback");
				LOG.error("notifyListener(T response) called twice", new IllegalStateException());
				if(DEBUG_CALLPATH)
					LOG.error("Previous call", prevCallStack);
				return;
			}
			finished=true;
		}
		if(DEBUG_CALLPATH)
			prevCallStack = new IllegalStateException("Originall call stack");
		try {
			onComplete(response);
		} catch (Throwable e) {
			LOG.error("IGNORING Exception in operationCompleted()", e);
		}

	}
	public final void notifyListener(Throwable throwable) {
		synchronized (this) {
			if(finished) {
				LOG.error("notifyListener(T response) called on finished AsyncCallback");
				LOG.error("notifyListener(T response) called twice", new IllegalStateException());
				if(DEBUG_CALLPATH)
					LOG.error("Previous call", prevCallStack);
				return;
			}
			finished=true;
		}
		if(DEBUG_CALLPATH)
			prevCallStack = new IllegalStateException("Originall call stack");
		try {
			onError(throwable);
		} catch (Throwable e) {
			LOG.error("IGNORING Exception in operationCompleted()", e);
		}
	}

	
	protected abstract void onComplete(T response);
	protected void onError(Throwable throwable) {
		onComplete(null);
	}
	
//	public boolean isFinished() {
//		synchronized (this) {
//			return finished;
//		}
//	}
}
