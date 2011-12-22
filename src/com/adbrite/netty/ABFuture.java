package com.adbrite.netty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wait synchronously on a asynchronous service.
 * Useful for testing and creating compatibility layers between old sync and new async code 
 * Usage:
 * <pre>
 *		ABFuture<Zone> future = new ABFuture<Zone>();
 *		abrom.lookupZone(pZoneSid, future);
 *		Zone z = future.get();
 * </pre>
 * Please note that we always use a timeout to avoid resource leak 
 * @author apesternikov
 *
 * @param <V>
 */
public class ABFuture<V> extends AsyncCallback<V> implements Future<V> {
	private static final int DEFAULT_TIMEOUT = 20;
	private volatile V result;
	private volatile boolean done=false;
	// FIXME: countdown latch is quite heavy creature. Consider Object.wait()
	private CountDownLatch latch = new CountDownLatch(1);

	/**
	 * Set result and notify waiters
	 */
	@Override
	public void onComplete(V result) {
		this.result = result;
		done=true;
		latch.countDown();
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	/**
	 * Original Future implementation supposed to wait forever.
	 * We are aborting this process in 20 seconds and throwing a RuntimeException
	 * @throws RuntimeException, InterruptedException
	 */
	@Override
	public V get() throws InterruptedException{
		if(!latch.await(DEFAULT_TIMEOUT, TimeUnit.SECONDS))
			throw new RuntimeException("Default ABFuture waiting time exceeded");
		return result;
	}
	
	/**
	 * Version of get() that doesn't throw exception but returns null instead
	 * @return
	 * @throws RuntimeException
	 */
	public V getOrNull() {
		try {
			return get();
		} catch (InterruptedException e) {
			return null;
		}
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(latch.await(timeout, unit))
			return result;
		throw new TimeoutException();
	}


}
