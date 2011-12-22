package com.adbrite.netty.httpd;

import java.util.concurrent.Executor;

import javax.annotation.Resource;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ControllerHandler extends SimpleChannelUpstreamHandler {
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpDispatchHandler.class);
	private Controller c;

	private Executor workerExecutor;

	public ControllerHandler(Controller controller, Executor executor) {
		c = controller;
		workerExecutor = executor;
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent evt)
			throws Exception {
		HttpRequest _request = (HttpRequest) evt.getMessage();
		final ABHttpRequest req = new ABHttpRequest(_request,
				evt.getChannel());
		final Channel channel = evt.getChannel();
		workerExecutor.execute(new Runnable() {
			@Override
			public void run() {
				ABHttpResponse resp = new ABHttpResponse();
				HttpResponse r;
				try {
					c.processRequest(req, resp);
					r=resp;
				} catch (Exception e) {
					LOG.error("Exception while calling http controller", e);
					r = new HttpResponse500();
				}
				channel.write(r);
			}
		});
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		LOG.error("ControllerHandler exception", e);
		Channel channel = e.getChannel();
		if(channel.isConnected())
			channel.close();
	}

}
