package com.adbrite.netty.httpd;

import org.jboss.netty.handler.codec.http.HttpResponse;

import com.adbrite.netty.AsyncCallback;

/**
 * Simple (= adserver-specific stuff like visitor is not required) async handler.
 * If you are looking for a base for converted adserver's ControllerBase based controllers AsyncThrottledHandler is probably what you are looking for
 * 
 * @author apesternikov
 *
 */
public interface AsyncController {

	/**
	 * Start async request. Controller should call responseHandler.onComplete(response) to actually send the response.
	 * @param request
	 * @param responseHandler asynchronous handler to send response back to browser
	 * Anything thrown from the controller will cause 500 response.
	 */
	void processAsyncRequest(ABHttpRequest request, AsyncCallback<HttpResponse>responseHandler) throws Exception ;
}
