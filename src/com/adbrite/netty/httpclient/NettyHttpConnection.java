package com.adbrite.netty.httpclient;

import java.net.UnknownHostException;
import java.util.concurrent.Executor;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.jmx.annotations.ManagedAttribute;
import com.adbrite.netty.AsyncCallback;
import com.adbrite.netty.NettyPersistentConnection;
import com.adbrite.netty.ProtocolException;

public class NettyHttpConnection extends NettyPersistentConnection<HttpOperation> implements HttpClient{

	private static final Logger LOG = LoggerFactory.getLogger(NettyHttpConnection.class);

	@ManagedAttribute(description="Keepalive support detected")
	private boolean canKeepAlive=false;
	@Override
	protected Logger getLog() {
		return LOG;
	}

	public NettyHttpConnection(String host, int port,
			Executor bossExecutor, Executor workerExecutor, int poolNum) throws UnknownHostException {
		super(host, port, bossExecutor, workerExecutor, "com.adbrite.netty.httpclient:type=NettyHttpConnection,address="+host+"-"+port+",poolNum="+poolNum);
		setFirstReconnectionTimeout(20);
		maxOps=1; //no keepalive
	}

	@Override
	protected ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = Channels.pipeline();

//        if (requestCompressionLevel > 0) {
//            pipeline.addLast("deflater", new HttpContentCompressor(requestCompressionLevel));
//        }

        pipeline.addLast("codec", new HttpClientCodec(4096, 328768, 1024*1024 ));
//        if (autoInflate) {
//            pipeline.addLast("inflater", new HttpContentDecompressor());
//        }
//        if (aggregateResponseChunks) {
            pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
//        }
        return pipeline;
	}

	@Override
	protected void initDecoder() {
	}

	@Override
	protected void onMessage(Object message, MessageEvent e)
			throws ProtocolException {
		if (message instanceof HttpResponse) {
			HttpResponse resp = (HttpResponse) message;
	        HttpOperation op = opq.poll();
			if(LOG.isDebugEnabled()) {
				LOG.debug(e.getRemoteAddress().toString()+"-->"+resp.toString());
			}
			if(op==null)
				throw new ProtocolException("No more op handlers to handle response");
			op.readFromBuffer(resp);
			if(!HttpHeaders.isKeepAlive(resp)) {
				Channel c = channel;
				channel=null;
				c.close();
			} else {
				if(!canKeepAlive) {
					canKeepAlive = true;
					maxOps=100;
				}
			}
			op.notifyListener();
		}
	}
	
	@Override
	public void execute(HttpRequest request, int timeoutMs, final AsyncCallback<HttpResponse> callback) {
		execute(new HttpOperation(request, timeoutMs) {
			@Override
			public void operationCompleted() {
				if(isSucess()) {
					callback.notifyListener(getResponse());
				} else {
					callback.notifyListener(getFailureReason());
				}
			}
		});
	}


}
