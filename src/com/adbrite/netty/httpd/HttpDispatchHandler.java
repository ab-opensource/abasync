package com.adbrite.netty.httpd;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDispatchHandler extends SimpleChannelHandler {
	private static final String HTTPHANDLER_NAME = "httphandler";

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpDispatchHandler.class);

	private final HttpServer server;

	private volatile long startTime;
	private volatile HttpDispatchTable.Item item;

	public HttpDispatchHandler(HttpServer server) {
		this.server = server;
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent evt)
			throws Exception {
		HttpRequest _request = (HttpRequest) evt.getMessage();
		server.requestsReceived.incrementAndGet();
		if(LOG.isDebugEnabled())
			LOG.debug("Request: {}", _request);

		String requestUri = _request.getUri();
		HttpDispatchTable.Item item = server.getDispatchTable().get(requestUri);
		item.count.incrementAndGet();
		item.inProgress.incrementAndGet();
		startTime = System.nanoTime();
		ChannelUpstreamHandler handler = item.handler;
		this.item = item;
		ChannelPipeline pipeline = context.getChannel().getPipeline();
		pipeline.addLast(HTTPHANDLER_NAME, handler);
		evt.getChannel().setReadable(false);
		context.sendUpstream(evt);
	}
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object o = e.getMessage();
		if(o instanceof HttpMessage) {
			item.inProgress.decrementAndGet();
			ctx.getPipeline().remove(HTTPHANDLER_NAME);
			long endTime = System.nanoTime();
			item.avgLatencyMs.add(TimeUnit.NANOSECONDS.toMillis(endTime-startTime));
		}
		super.writeRequested(ctx, e);
	}
	//Note to developer:
	//You better override exceptionCaught in your handler to avoid unnecessary logging
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		Throwable cause = e.getCause();
		if( cause instanceof ClosedChannelException
				|| cause instanceof IOException
				) {
			return;
		}
		super.exceptionCaught(ctx, e);
	}
	
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		if(LOG.isDebugEnabled())
			LOG.debug("Http connection accepted from {}", e.getChannel().getRemoteAddress());
		//all it to channel group for statistics
		server.httpChannels.add(e.getChannel());
		server.connectionsAccepted.incrementAndGet();
		super.channelConnected(ctx, e);
	}
	
}
