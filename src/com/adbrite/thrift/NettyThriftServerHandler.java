package com.adbrite.thrift;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.apache.thrift.TProcessor;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.cgbystrom.netty.thrift.ThriftServerHandler;


public class NettyThriftServerHandler extends ThriftServerHandler {
	private static Logger LOG = LoggerFactory.getLogger(ThriftServer.class);
	private ThriftServer server;


	public NettyThriftServerHandler(ThriftServer thriftServer) {
		super(thriftServer.processor);
		this.server = thriftServer;
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		Throwable cause = e.getCause();
		if( cause instanceof ClosedChannelException
				|| cause instanceof IOException
				) {
			return;
		}
		LOG.error("Exception:", cause);
		e.getChannel().close();
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		server.channels.add(e.getChannel());
		server.connectionsAccepted.incrementAndGet();
		super.channelConnected(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		server.requestsReceived.incrementAndGet();
		super.messageReceived(ctx, e);
	}
	
}
