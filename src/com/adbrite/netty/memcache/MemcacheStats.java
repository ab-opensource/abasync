package com.adbrite.netty.memcache;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.ReadOnlyChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.netty.NettyUtils;
import com.adbrite.netty.ProtocolException;

public abstract class MemcacheStats extends MemcacheOperation {
	
	private static final Logger LOG = LoggerFactory.getLogger(MemcacheStats.class);

	private static final ReadOnlyChannelBuffer REQ = new ReadOnlyChannelBuffer(ChannelBuffers.wrappedBuffer(new byte[] {'s', 't', 'a', 't', 's', '\r', '\n'}));
	
	private static final String END = "END";
	private static final String STAT = "STAT";

	@Override
	public ReadOnlyChannelBuffer resetAndBuildRequest() {
		return REQ;
	}
	
	//private List<String> statlines = new ArrayList<String>(20);

	private Map<String, String> values = new HashMap<String, String>();
	
	
	@Override
	public boolean readFromBuffer(ChannelBuffer data) throws ProtocolException, NoSuchElementException {
		while(true) {
			String s = NettyUtils.readStringFromBuffer(data);
			if(s==null)
				return false;
			if(LOG.isDebugEnabled())
				LOG.debug("stats response: "+s);
			if(END.equals(s)) {
				return true;
			}
			StringTokenizer t = new StringTokenizer(s);
			String cmd = t.nextToken();
			if(STAT.equals(cmd)) {
				String n = t.nextToken();
				String v = t.nextToken();
				//nextToken throws NoSuchElementException, we catch it in upper level
				//statlines.add(s.toString(CHARSET));
				values.put(n, v);
			}
			else throw new ProtocolException("Unexpected line "+s);
		}
	}
	
	public Map<String, String> getValues() {
		return values ;
	}

	@Override
	public String toString() {
		return "STATS";
	}
}
