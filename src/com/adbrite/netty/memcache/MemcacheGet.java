package com.adbrite.netty.memcache;

import static com.adbrite.netty.memcache.NettyMemcacheConnection.CHARSET;

import java.util.StringTokenizer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.ReadOnlyChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.netty.NettyUtils;
import com.adbrite.netty.ProtocolException;

public abstract class MemcacheGet extends MemcacheSingleKeyOperation {

	private static final NotFoundException NOT_FOUND = new NotFoundException();

	private static final Logger LOG = LoggerFactory.getLogger(MemcacheGet.class);

	private static final String RESPONSE_END = "END";
	private static final String RESPONSE_VALUE = "VALUE ";
	private static final byte[] COMMAND_GET = "get ".getBytes();
	private static final byte[] CRLF = "\r\n".getBytes();

	private final NormalizedKey key;
	private final ReadOnlyChannelBuffer REQ;

	private byte[] value;
	
	private enum State {
		READ_CMD, READ_DATA, DONE
	}

	private State state;

	private int expecting_bytes = 0;
	
	public MemcacheGet(String key) {
		this(new NormalizedKey(key));
	}

	// TODO: consider changing key and value type to binary?
	public MemcacheGet(NormalizedKey key) {
		this.key = key;
		ChannelBuffer b = ChannelBuffers.dynamicBuffer(key.length() + 10);
		b.writeBytes(COMMAND_GET);
		b.writeBytes(key.getBytes());
		b.writeBytes(CRLF);
		REQ = new ReadOnlyChannelBuffer(b);
		LOG.debug("Request: "+REQ.toString(CHARSET));

	}

	@Override
	public ReadOnlyChannelBuffer resetAndBuildRequest() {
		value = null;
		state = State.READ_CMD;
		return REQ;
	}

	@Override
	protected
	boolean readFromBuffer(ChannelBuffer data) throws ProtocolException {
		while (true) {
			next: switch (state) {
			case READ_CMD:
				String s = NettyUtils.readStringFromBuffer(data);
				if (s == null)
					return false;
				if (s.isEmpty()) {
					throw new ProtocolException("line should not be empty");
				}
				if(LOG.isDebugEnabled())
					LOG.debug(s);
				char firstbyte = s.charAt(0);
				switch (firstbyte) {
				case 'E':
					if (s.equals(RESPONSE_END)) {
						if (value == null)
							notifyListener(NOT_FOUND);
						state = State.DONE;
						if(LOG.isDebugEnabled())
							LOG.debug("done");
						return true; // done
					}
					break;
				case 'V':
					if (s.startsWith(RESPONSE_VALUE)) {
						StringTokenizer t = new StringTokenizer(s);
						@SuppressWarnings("unused")
						String cmd = t.nextToken();
						String key = t.nextToken();
						@SuppressWarnings("unused")
						String flags = t.nextToken();
						String bytes = t.nextToken();
						if (!key.equals(this.key.toString())) {
							throw new ProtocolException("expected key "
									+ this.key + " got key " + key);
						}
						expecting_bytes = Integer.parseInt(bytes);
						state = State.READ_DATA;
						break next; // want next line
					}
					break;
				}
				throw new ProtocolException("Unexpected line "+ s);

			case READ_DATA:
				if(LOG.isDebugEnabled())
					LOG.debug("reading data");
				if (data.readableBytes() < expecting_bytes + 2)
					return false; // want more
				value = new byte[expecting_bytes];
				data.readBytes(value);
				if (!NettyUtils.startsWith(data, CRLF))
					throw new ProtocolException("Expecting CRLF after data");
				data.skipBytes(2);
				state = State.READ_CMD;
				break next;
			default:
				throw new ProtocolException("Unexpected line "
						+ data.toString(CHARSET));

			}
		}
	}

	public String getValue() {
		if(value!=null)
			return new String(value);
		else
			return null;
	}

	@Override
	public NormalizedKey getKey() {
		return key;
	}
	@Override
	public String toString() {
		return "GET "+key;
	}
}
