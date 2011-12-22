package com.adbrite.netty.httpd;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponceHeadersManager extends SimpleChannelDownstreamHandler implements ChannelUpstreamHandler {

	private static final Logger LOG = LoggerFactory
	.getLogger(HttpResponceHeadersManager.class);

	static private TimeZone gmt = TimeZone.getTimeZone("GMT");
	private SimpleDateFormat format;

	{
		format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		format.setTimeZone(gmt);
	}
	private boolean enableKeepAlive;
	private HttpVersion reqVersion;

	public HttpResponceHeadersManager(boolean enableKeepAlive) {
		this.enableKeepAlive=enableKeepAlive;
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent evt)
			throws Exception {
		Object o = evt.getMessage();
		if (o instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) evt.getMessage();
			response.setProtocolVersion(reqVersion);
			if (noBodyStatus(response.getStatus())) {
				response.setContent(ChannelBuffers.EMPTY_BUFFER);
				response.removeHeader(Names.CONTENT_LENGTH);
			} else {
				int len = response.getContent().readableBytes();
				HttpHeaders.setContentLength(response, len);
			}
			HttpHeaders.setKeepAlive(response, enableKeepAlive);
			if(!enableKeepAlive) {
				evt.getFuture().addListener(ChannelFutureListener.CLOSE);
			}
			evt.getChannel().setReadable(true);
			response.setHeader(HttpHeaders.Names.SERVER, "XPEHOTEHb/1.1");
			response.setHeader(HttpHeaders.Names.ACCEPT_RANGES, "none");
			Date d = new Date();
			String df = format.format(d);
			response.setHeader(HttpHeaders.Names.DATE, df);
		}
		super.writeRequested(ctx, evt);
	}

	private boolean noBodyStatus(HttpResponseStatus status) {
		return status == HttpResponseStatus.NO_CONTENT
				|| status == HttpResponseStatus.NOT_MODIFIED;
	}

	// Upstream part. 
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
			throws Exception {
	        if (e instanceof MessageEvent) {
	            messageReceived(ctx, (MessageEvent) e);	
	        }else {
	            ctx.sendUpstream(e);
	        }
	}

	public void messageReceived(
            ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object o = e.getMessage();
		if (o instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) o;
			reqVersion = req.getProtocolVersion();
			if(enableKeepAlive)
				enableKeepAlive = HttpHeaders.isKeepAlive(req);
		}
        ctx.sendUpstream(e);
    }

}
