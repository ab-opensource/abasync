package com.adbrite.netty.memcache;

import org.jboss.netty.buffer.ChannelBuffer;

import com.adbrite.netty.NetworkOperation;
import com.adbrite.netty.ProtocolException;

public abstract class MemcacheOperation extends NetworkOperation<ChannelBuffer, ChannelBuffer>{
	private static final int DEFAULT_TIMEOUT = 40;
	private static int timeoutMs = DEFAULT_TIMEOUT;

	public MemcacheOperation(int timeoutMs) {
		super(timeoutMs);
	}

	public MemcacheOperation() {
		this(timeoutMs);
	}

	//make it visible to NettyMemcacheConnection
	@Override
	abstract protected boolean readFromBuffer(ChannelBuffer data) throws ProtocolException;

	public static int getTimeoutMs() {
		return timeoutMs;
	}

	public static void setTimeoutMs(int timeoutMs) {
		MemcacheOperation.timeoutMs = timeoutMs;
	}

}
