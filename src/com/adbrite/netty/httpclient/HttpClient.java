package com.adbrite.netty.httpclient;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.adbrite.netty.AsyncCallback;

public interface HttpClient {
	public boolean isAvailable();
	void execute(HttpRequest request, int timeoutInMs, AsyncCallback<HttpResponse> callback);
	void stop();
	void setAddress(String host, int port);
	double getFailureRate();
}
