package com.adbrite.netty.httpd;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class HttpResponseRedirect extends DefaultHttpResponse {

	private static ChannelBuffer content;
	static {
		String s = "Moved";
		content=ChannelBuffers.unmodifiableBuffer(ChannelBuffers.wrappedBuffer(s.getBytes()));
	}

	/**
	 * use HttpResponseBounce.INSTANCE instead
	 */
	public HttpResponseRedirect(String location) {
		super(HttpVersion.HTTP_1_1,	HttpResponseStatus.MOVED_PERMANENTLY);
		setHeader(HttpHeaders.Names.LOCATION, location);
		setContent(content);
	}

}
