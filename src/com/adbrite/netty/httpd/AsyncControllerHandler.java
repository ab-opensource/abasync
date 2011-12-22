package com.adbrite.netty.httpd;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.netty.AsyncCallback;

public class AsyncControllerHandler extends SimpleChannelUpstreamHandler {
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpDispatchHandler.class);
	private AsyncController c;

	public AsyncControllerHandler(AsyncController controller) {
		c = controller;
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, final MessageEvent evt)
			throws Exception {
		HttpRequest _request = (HttpRequest) evt.getMessage();
		ABHttpRequest req = new ABHttpRequest(_request,	evt.getChannel());
		AsyncCallback<HttpResponse> responseHandler = new AsyncCallback<HttpResponse>() {
			
			@Override
			public void onError(Throwable throwable) {
				LOG.error("ControllerHandler exception", throwable);
				Channel channel = evt.getChannel();
					channel.write(new HttpResponse500());
			}
			
			@Override
			public void onComplete(HttpResponse response) {
				evt.getChannel().write(response);
			}
		};
		c.processAsyncRequest(req, responseHandler );
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		LOG.error("ControllerHandler exception", e.getCause());
		Channel channel = e.getChannel();
		if(channel.isConnected())
			channel.write(new HttpResponse500());
	}

}
