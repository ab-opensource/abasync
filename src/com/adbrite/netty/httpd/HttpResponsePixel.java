package com.adbrite.netty.httpd;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.adbrite.util.ABUtils;

public class HttpResponsePixel extends DefaultHttpResponse {

	private static ChannelBuffer content;
	static {
		content=ChannelBuffers.unmodifiableBuffer(ChannelBuffers.wrappedBuffer(ABUtils.BLANK_GIF_BYTES));
	}

	public HttpResponsePixel() {
		super(HttpVersion.HTTP_1_1,	HttpResponseStatus.OK);
		setHeader(HttpHeaders.Names.CONTENT_TYPE, "image/gif");
		setContent(content);
	}

}
