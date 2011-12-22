package com.adbrite.netty.memcache;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.ReadOnlyChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.netty.NettyUtils;
import com.adbrite.netty.ProtocolException;


public class MemcacheSet extends MemcacheSingleKeyOperation {
	private static final NotStoredException NOT_STORED_EXCEPTION = new NotStoredException();

	private static final NotFoundException NOT_FOUND_EXCEPTION = new NotFoundException();

	private static final KeyExistsException KEY_EXISTS_EXCEPTION = new KeyExistsException();

	private static final Logger LOG = LoggerFactory.getLogger(MemcacheSet.class);

	private static final String RESPONSE_STORED = "STORED";
	private static final String RESPONSE_EXISTS = "EXISTS";
	private static final String RESPONSE_NOT_STORED = "NOT_STORED";
	private static final String RESPONSE_NOT_FOUND = "NOT_FOUND";
	private static final byte[] CRLF = new byte[] {'\r', '\n'};

	public enum Commands {
		SET,
		ADD,
		REPLACE,
		APPEND,
		PREPEND;

		private final byte[] bytes;

		private Commands() {
			bytes = (name().toLowerCase() + ' ').getBytes();
		}

		public byte[] getBytes() {
			return bytes;
		}
	}

	private final ReadOnlyChannelBuffer REQ;

	private NormalizedKey key;

	public MemcacheSet(String key, String value, long expire) {
		this(new NormalizedKey(key), value, expire);
	}
	public MemcacheSet(NormalizedKey key, String value, long expire)
	{
		this(key, value, 0, expire);
	}
	public MemcacheSet(String key, String value, long flags, long expire)
	{
		this(new NormalizedKey(key), value, flags, expire);
	}
	//TODO: consider changing key and value type to binary?
	public MemcacheSet(NormalizedKey key, String value, long flags, long expire)
	{
		this(Commands.SET, key, flags, expire, value.getBytes());
	}

	public MemcacheSet(Commands command, NormalizedKey key, long flags, long expire, byte[] bytes) {
		this.key = key;
		ChannelBuffer r = ChannelBuffers.dynamicBuffer(key.length() + bytes.length + 50);
		r.writeBytes(command.getBytes());
		r.writeBytes(key.getBytes());
		r.writeByte(' ');
		r.writeBytes(Long.toString(flags).getBytes());
		r.writeByte(' ');
		r.writeBytes(Long.toString(expire).getBytes());
		r.writeByte(' ');
		r.writeBytes(Integer.toString(bytes.length).getBytes());
		r.writeBytes(CRLF);
		r.writeBytes(bytes);
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
		case 'S':
			if(s.equals(RESPONSE_STORED)) {
				return true; //done
			}
			break;
		case 'E':
			if(s.equals(RESPONSE_EXISTS)) {
				notifyListener(KEY_EXISTS_EXCEPTION);
				return true; //done
			}
			break;
		case 'N':
			if(s.equals(RESPONSE_NOT_FOUND)) {
				notifyListener(NOT_FOUND_EXCEPTION);
				return true;
			} else if(s.equals(RESPONSE_NOT_STORED)) {
				notifyListener(NOT_STORED_EXCEPTION);
				return true; //done
			}
			break;
		}
		throw new ProtocolException("Unexpected line "+s);
	}
	
	@Override
	public NormalizedKey getKey() {
		return key;
	}

	/**
	 * Default completion callback is empty, we usually don't care about set completion status
	 */
	@Override
	public void operationCompleted() {
	}

}
