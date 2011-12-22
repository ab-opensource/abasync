package com.adbrite.netty.httpd;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class Http404Handler extends SimpleChannelUpstreamHandler {
	private static HttpResponse404 resp = new HttpResponse404();
	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent evt)
			throws Exception {
		evt.getChannel().write(resp);
	}
}
