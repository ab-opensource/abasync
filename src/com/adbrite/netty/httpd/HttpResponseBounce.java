package com.adbrite.netty.httpd;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class HttpResponseBounce extends DefaultHttpResponse {

	private static final String BOUNCE_URL = "http://bounce.adbrite.com/";
	private static ChannelBuffer content;
	static {
		String s = "Moved";
		content=ChannelBuffers.unmodifiableBuffer(ChannelBuffers.wrappedBuffer(s.getBytes()));
	}

	/**
	 * use HttpResponseBounce.INSTANCE instead
	 */
	public HttpResponseBounce() {
		super(HttpVersion.HTTP_1_1,	HttpResponseStatus.MOVED_PERMANENTLY);
		setHeader(HttpHeaders.Names.LOCATION, BOUNCE_URL);
		setContent(content);
	}

}
