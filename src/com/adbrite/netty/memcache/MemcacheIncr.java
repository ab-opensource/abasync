package com.adbrite.netty.memcache;

import java.math.BigInteger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.ReadOnlyChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.netty.NettyUtils;
import com.adbrite.netty.ProtocolException;

public class MemcacheIncr extends MemcacheSingleKeyOperation {
	private static final NotFoundException NOT_FOUND_EXCEPTION = new NotFoundException();

	private static final Logger LOG = LoggerFactory.getLogger(MemcacheIncr.class);

	private static final String RESPONSE_NOT_FOUND = "NOT_FOUND";
	private static final byte[] CRLF = new byte[] {'\r', '\n'};

	private final BigInteger U64_MASK = BigInteger.ZERO.not().shiftLeft(Long.SIZE).not();

	public enum Commands {
		INCR,
		DECR;

		private final byte[] bytes;

		private Commands() {
			bytes = (name().toLowerCase() + ' ').getBytes();
		}

		public byte[] getBytes() {
			return bytes;
		}
	}

	private final ReadOnlyChannelBuffer REQ;

	private final NormalizedKey key;
	private long newValue;
	
	public MemcacheIncr(String key, long amount) {
		this(amount >= 0 ? Commands.INCR : Commands.DECR, new NormalizedKey(key), Math.abs(amount));
	}

	public MemcacheIncr(Commands command, NormalizedKey key, long amount) {
		this.key = key;
		ChannelBuffer r = ChannelBuffers.dynamicBuffer(key.length() + 50);
		r.writeBytes(command.getBytes());
		r.writeBytes(key.getBytes());
		r.writeByte(' ');
		r.writeBytes(U64_MASK.and(BigInteger.valueOf(amount)).toString().getBytes());
		r.writeBytes(CRLF);
		REQ = new ReadOnlyChannelBuffer(r);
	}

	@Override
	public ReadOnlyChannelBuffer resetAndBuildRequest() {
		return REQ;
	}

	@Override
	public boolean readFromBuffer(ChannelBuffer data) throws ProtocolException {
		String s = NettyUtils.readStringFromBuffer(data);
		if(s==null)
			return false;
		if(s.isEmpty()) {
			throw new ProtocolException("line should not be empty");
		}
		char firstbyte = s.charAt(0);
		switch (firstbyte) {
		case 'N':
			if(s.equals(RESPONSE_NOT_FOUND)) {
				notifyListener(NOT_FOUND_EXCEPTION);
				return true;
			}
			break;
		default:
			newValue = U64_MASK.and(new BigInteger(s)).longValue();
			notifyListener();
			return true;
		}
		throw new ProtocolException("Unexpected line "+s);
	}

	@Override
	public NormalizedKey getKey() {
		return key;
	}

	public long getNewValue() {
		return newValue;
	}

	/**
	 * Default completion callback is empty, we usually don't care about set completion status
	 */
	@Override
	public void operationCompleted() {
	}

}
