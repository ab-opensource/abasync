package com.adbrite.netty.httpd;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class HttpResponse400 extends DefaultHttpResponse {

	public HttpResponse400() {
		super(HttpVersion.HTTP_1_1,	HttpResponseStatus.NOT_FOUND);
		setHeader(HttpHeaders.Names.CONTENT_LENGTH, new Integer(0));
	}

}
