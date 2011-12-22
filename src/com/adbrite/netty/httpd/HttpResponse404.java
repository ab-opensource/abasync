package com.adbrite.netty.httpd;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class HttpResponse404 extends DefaultHttpResponse {

	private static ChannelBuffer content;
	static {
		//TODO: reall html page here
		String s = "Not Found";
		content=ChannelBuffers.unmodifiableBuffer(ChannelBuffers.wrappedBuffer(s.getBytes()));
	}

	public HttpResponse404() {
		super(HttpVersion.HTTP_1_1,	HttpResponseStatus.NOT_FOUND);
		setContent(content);
		setHeader(HttpHeaders.Names.CONTENT_LENGTH, new Integer(getContent().readableBytes()));
	}

}
