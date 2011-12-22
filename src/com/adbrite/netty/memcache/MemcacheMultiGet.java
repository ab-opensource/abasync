package com.adbrite.netty.memcache;

import static com.adbrite.netty.memcache.NettyMemcacheConnection.CHARSET;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.ReadOnlyChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.netty.NettyUtils;
import com.adbrite.netty.NetworkOperation;
import com.adbrite.netty.ProtocolException;

/**
 * @deprecated DO NOT USE IT, scheduled for removal. Use MemcacheGet with barrier
 * @author apesternikov
 *
 */
@Deprecated
public abstract class MemcacheMultiGet extends MemcacheOperation {

	private static final Logger LOG = LoggerFactory.getLogger(MemcacheMultiGet.class);

	private static final String RESPONSE_END = "END";
	private static final String RESPONSE_VALUE = "VALUE";
	private static final byte[] COMMAND_GET = "get".getBytes();
	private static final byte[] CRLF = "\r\n".getBytes();

	private final NormalizedKey[] keys;
	private final ReadOnlyChannelBuffer REQ;
	
	private Map<String,byte[]> values;

	private enum State {
		READ_CMD, READ_DATA, DONE
	}

	private State state;

	private int expecting_bytes = 0;

	private String currentKey;

	public MemcacheMultiGet(Collection<String> keys) {
		this(keys.toArray(new String[keys.size()]));
	}
	public MemcacheMultiGet(String ... keys) {
		this.keys = new NormalizedKey[keys.length];
		for(int i=0; i<keys.length; i++)
			this.keys[i] = new NormalizedKey(keys[i]);
		ChannelBuffer b = ChannelBuffers.dynamicBuffer();
		b.writeBytes(COMMAND_GET);
		for (String key : keys) {
			b.writeByte(' ');
			b.writeBytes(key.getBytes());
		}
		b.writeBytes(CRLF);
		REQ = new ReadOnlyChannelBuffer(b);
		LOG.debug("Request: "+REQ.toString(CHARSET));

	}

	@Override
	public ReadOnlyChannelBuffer resetAndBuildRequest() {
		values = new HashMap<String, byte[]>(keys.length);
		state = State.READ_CMD;
		return REQ;
	}

	@Override
	public boolean readFromBuffer(ChannelBuffer data) throws ProtocolException {
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
						state = State.DONE;
						if(LOG.isDebugEnabled())
							LOG.debug("done");
						return true; // done
					}
					break;
				case 'V':
					if (s.startsWith(RESPONSE_VALUE)) {
						StringTokenizer t = new StringTokenizer(s);
						String cmd = t.nextToken();
						currentKey = t.nextToken();
						@SuppressWarnings("unused")
						String flags = t.nextToken();
						String bytes = t.nextToken();
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
				byte[] value = new byte[expecting_bytes];
				data.readBytes(value);
				if (!NettyUtils.startsWith(data, CRLF))
					throw new ProtocolException("Expecting CRLF after data");
				data.skipBytes(2);
				values.put(currentKey, value);
				state = State.READ_CMD;
				break next;
			default:
				throw new ProtocolException("Unexpected line "
						+ data.toString(CHARSET));

			}
		}
	}

//	public Map<String, byte[]> getValues() {
//		return values;
//	}
	
	public boolean containsKey(String key) {
		return values.containsKey(key);
	}
	
	public byte[] getValue(String key) {
		return values.get(key);
	}

	public String getStringValue(String key) {
		byte[] bv = values.get(key);
		if(bv!=null)
			return new String(bv);
		return null;
	}

}
