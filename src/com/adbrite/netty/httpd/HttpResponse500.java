package com.adbrite.netty.httpd;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class HttpResponse500 extends DefaultHttpResponse {

	public HttpResponse500() {
		this("Server Error");
	}
	public HttpResponse500(String contentString) {
		super(HttpVersion.HTTP_1_1,	HttpResponseStatus.INTERNAL_SERVER_ERROR);
		ChannelBuffer content=ChannelBuffers.unmodifiableBuffer(ChannelBuffers.wrappedBuffer(contentString.getBytes()));
		setContent(content);
		setHeader(HttpHeaders.Names.CONTENT_LENGTH, new Integer(getContent().readableBytes()));
	}
	

}
