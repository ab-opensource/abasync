package com.adbrite.netty.httpd;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelUpstreamHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class IdleConnectionKiller extends IdleStateAwareChannelUpstreamHandler {
	private static final Logger LOG = LoggerFactory
			.getLogger(IdleConnectionKiller.class);

	@Override
	public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
			throws Exception {
		if (LOG.isDebugEnabled())
			LOG.debug("Connection killed - timeout");
		ctx.getChannel().close();
	}
}
