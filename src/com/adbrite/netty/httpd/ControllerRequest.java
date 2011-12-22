package com.adbrite.netty.httpd;

import java.net.InetAddress;
import java.util.Map;

import javax.servlet.http.Cookie;

public interface ControllerRequest {

	/**
	 * use getRemoteIP if possible
	 * @return
	 */
	public abstract String getRemoteAddr();
	public InetAddress getRemoteIP();

	public abstract String getRemoteHost();

	public abstract String getRequestURI();

	public abstract String getParameter(String parameterName);

	public abstract String getHeader(String httpRequestHeaderFieldName);

	public abstract InetAddress getLocalIP();

	public abstract boolean isSecure();

	public abstract int getServerPort();

	@SuppressWarnings("unchecked")
	public abstract Map<String, String[]> getParameterMap();

	public abstract Object getAttribute(String name);

	public abstract Cookie[] getCookies();

	public abstract String getQueryString();

	public abstract String getServerName();

	public abstract long getStartTimeMillis();

	public abstract long getProcessingTimeInMillis();

	public abstract boolean isAuctionSimVisitor();

	public abstract InetAddress getVisitorIpAddress();

	/**
	 * Get visitor IP address. IP address could be faked using 'abipfake' URL
	 * parameter in auction simulator or specified by Akamai-Visitor-IP header.
	 *
	 * @param httpRequest
	 * @return
	 */
	public abstract InetAddress getVisitorIpAddress(boolean realVisitor);
	
	/**
	 * Unique request id
	 * @return
	 */
	public String getRequestId();

}