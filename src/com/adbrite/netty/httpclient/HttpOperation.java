package com.adbrite.netty.httpclient;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.Timeout;

import com.adbrite.netty.NetworkOperation;
import com.adbrite.netty.ProtocolException;

public abstract class HttpOperation extends NetworkOperation<HttpRequest, HttpResponse> {
    public static final String X_ADBRITE_REQUEST_ID = "X-Adbrite-Request-ID";

	public static final int DEFAULT_HTTP_TIMEOUT = 3000;
	private final HttpRequest request;
	private final String id;
	private volatile HttpResponse response;
	public HttpOperation(HttpRequest request, int timeoutMs) {
		super(timeoutMs);
		this.request=request;
		id=request.getHeader(X_ADBRITE_REQUEST_ID);
	}
	public HttpOperation(HttpRequest request, int timeoutMs, int retryLimit) {
		super(timeoutMs, retryLimit);
		this.request=request;
		id=request.getHeader(X_ADBRITE_REQUEST_ID);
	}
	
	@Override
	protected HttpRequest resetAndBuildRequest() {
		return request;
	}

	@Override
	protected boolean readFromBuffer(HttpResponse data) throws ProtocolException {
		response = data;
		return true;
	}
	
	public HttpResponse getResponse() {
		return response;
	}

	//We want to close channel on timeout
	@Override
	public void run(Timeout timeout) throws Exception {
		super.run(timeout);
	}

	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return "HttpOperation_"+getId();
	}
}
