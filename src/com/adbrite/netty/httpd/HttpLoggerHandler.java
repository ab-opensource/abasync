package com.adbrite.netty.httpd;

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class HttpLoggerHandler extends SimpleChannelHandler {

	private HttpRequest request = null;
	private long startTime;

	private HttpLogger logger;
	private boolean enabled=true;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (enabled) {
			request = (HttpRequest) e.getMessage();
			startTime = System.nanoTime();
		}
		super.messageReceived(ctx, e);
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (enabled) {
			Object o = e.getMessage();
			if (o instanceof HttpMessage) {
				HttpResponse response = (HttpResponse) e.getMessage();
				Channel channel = ctx.getChannel();
				SocketAddress ra = channel.getRemoteAddress();
				logger.log(request, response, ra, startTime);
				if (!e.getChannel().isConnected()) // no reason to write if
													// nobody is listening
					return;
			}
		}
		super.writeRequested(ctx, e);
	}

	public HttpLoggerHandler(HttpLogger logger, boolean enabled) {
		this.logger = logger;
		this.enabled = enabled;
	}

}
