package com.adbrite.netty.memcache;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.jmx.annotations.MBean;
import com.adbrite.netty.NettyPersistentConnection;
import com.adbrite.netty.ProtocolException;

@MBean
public class NettyMemcacheConnection extends NettyPersistentConnection<MemcacheOperation> {
	static final Charset CHARSET = Charset.forName("ISO-8859-1");
	private static final Logger LOG = LoggerFactory.getLogger(NettyMemcacheConnection.class);
	@Override
	protected Logger getLog() {
		return LOG;
	}

	public NettyMemcacheConnection(InetSocketAddress address, Executor bossExecutor, Executor workerExecutor,String mbeanname) {
		super(address, bossExecutor, workerExecutor, mbeanname);
	}

	@Override
	protected ChannelPipeline getPipeline() {
		ChannelPipeline pipeline = pipeline();
		return pipeline;
	}

	protected void initDecoder() {
		buf.clear();
	}
	ChannelBuffer buf = ChannelBuffers.dynamicBuffer();

	protected void onMessage(Object m, MessageEvent e) throws ProtocolException {
        if (m instanceof ChannelBuffer) {
	        ChannelBuffer input = (ChannelBuffer) m;
	        if (!input.readable()) {
	            return;
	        }
	        
	        buf.writeBytes(input);
	        boolean done;
	        do {
		        MemcacheOperation op = opq.peek();
				if(LOG.isDebugEnabled()) {
					if(buf.readableBytes()<100)
						LOG.debug(e.getRemoteAddress().toString()+"-->"+buf.toString(CHARSET));
					else
						LOG.debug(e.getRemoteAddress().toString()+"--> buffer("+buf.readableBytes()+" bytes total)");
				}
				if(op==null)
					throw new ProtocolException("No more op handlers but still "+buf.readableBytes()+" bytes in buffer!");
				done = op.readFromBuffer(buf);
				if(done) {
					op.notifyListener();
		        	opq.poll();
		        }
	        } while(done && buf.readable()); //hand the remainder to the next handler
	        if(!buf.readable())
	        	buf.clear();  //reset read and write pointers to avoid memory allocation
        }
	}

}
