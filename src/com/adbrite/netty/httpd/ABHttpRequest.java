/**
 * 
 */
package com.adbrite.netty.httpd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.NotImplementedException;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.util.CharsetUtil;

import com.adbrite.util.ABUtils;
import com.google.common.base.Splitter;

public class ABHttpRequest implements HttpServletRequest, ControllerRequest {
	private static final String DEFAULT_CHAR_ENCODING = "UTF-8";
	private HttpRequest _request;
	private Map<String, Object> _attributes = new HashMap<String, Object>();
	private final Cookie[] _cookies;
	private final String _localIpAddr = null;
	private final boolean _secure ;
	private int _serverPort;
	private String _contextPath = ""; // substitute for Servlet API context path
	private final QueryString queryString;
	private final long startTimeMs;
	private final InetAddress remoteIP;
	private InetAddress localIP;
	
	private static Splitter ampersandSplitter = Splitter.on('&');

	private void parsePostBody(String qs) {
		for (String pair : ampersandSplitter.split(qs)) {
			String name;
			String value;
			int pos = pair.indexOf('=');
			// for "n=", the value is "", for "n", the value is null
			try {
				if (pos == -1) {
					name = URLDecoder.decode(pair, "UTF-8");
					value = null;
				} else {
					name = URLDecoder.decode(pair.substring(0, pos), "UTF-8");
					value = URLDecoder.decode(
							pair.substring(pos + 1, pair.length()), "UTF-8");
				}
				queryString.put(name, value);
			} catch (UnsupportedEncodingException e) {
				// Not really possible, throw unchecked
				throw new IllegalStateException("No UTF-8");
			} catch (IllegalArgumentException e) {
				// This exception is thrown on decoding error, ignore the parameter 
			}

		}
	}

	public ABHttpRequest(HttpRequest r, Channel channel) {
		// _servletName = servletName;
		// _contextPath = contextPath;
		startTimeMs = System.currentTimeMillis();
		_request = r;
		InetSocketAddress addr = (InetSocketAddress) channel.getLocalAddress();
		_serverPort = addr.getPort();
		if (0 == _serverPort) {
			_serverPort = 80;
		}
		String fakeip = r.getHeader("Source-IP");
		if(fakeip!=null) {
			InetAddress raddr;
			try {
				raddr = com.google.common.net.InetAddresses.forString(fakeip);
			} catch(IllegalArgumentException ex) {
				raddr = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
			}
			remoteIP = raddr;
		}
		else {
			remoteIP = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
		}
		localIP = ((InetSocketAddress) channel.getLocalAddress()).getAddress();
		_secure = (null != channel.getPipeline().get(SslHandler.class));
		List<String> cookieHeaders = r.getHeaders(HttpHeaders.Names.COOKIE);
		_cookies = CookieParser.parse(cookieHeaders);
		String uri = r.getUri();
		queryString = new QueryString(uri);
		queryString.put("serverIpAddress", addr.getAddress().getHostAddress());
		HttpMethod method = r.getMethod();
		if (method.equals(HttpMethod.POST)) {
			String postBody = r.getContent().toString(CharsetUtil.UTF_8);
			parsePostBody(postBody);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getHeaders(java.lang.String)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getHeaders(String name) {
		return Collections.enumeration(_request.getHeaders(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getHeaderNames()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getHeaderNames() {
		return Collections.enumeration(_request.getHeaderNames());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getHeader(java.lang.String)
	 */
	@Override
	public String getHeader(String name) {
		return _request.getHeader(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.adbrite.net.http.HttpServletRequest#getIntHeader(java.lang.String)
	 */
	@Override
	public int getIntHeader(String name) throws NumberFormatException {
		return Integer.parseInt(_request.getHeader(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getRequestURI()
	 */
	@Override
	public String getRequestURI() {
		return queryString.getPath();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#isSecure()
	 */
	@Override
	public boolean isSecure() {
		return _secure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.adbrite.net.http.HttpServletRequest#setAttribute(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void setAttribute(String name, Object value) {
		_attributes.put(name, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getRequestURL()
	 */
	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(queryString.getPath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getQueryString()
	 */
	@Override
	public String getQueryString() {
		return queryString.getQueryString();
	}

	/*
	 * (non-Javadoc)
	 * Use getRemoteIP instead if possible
	 * @see com.adbrite.net.http.HttpServletRequest#getRemoteAddr()
	 */
	@Override
	public String getRemoteAddr() {
		return remoteIP.getHostAddress();
	}
	
	public InetAddress getRemoteIP() {
		return remoteIP;
	}

	/*
	 * (non-Javadoc)
	 * Use getRemoteIP instead if possible
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getRemoteHost()
	 */
	@Override
	public String getRemoteHost() {
		return remoteIP.getHostAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.adbrite.net.http.HttpServletRequest#getParameter(java.lang.String)
	 */
	@Override
	public String getParameter(String name) {
		return queryString.getParameter(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getCookies()
	 */
	@Override
	public Cookie[] getCookies() {
		return _cookies;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.adbrite.net.http.HttpServletRequest#getParameterValues(java.lang.
	 * String)
	 */
	@Override
	public String[] getParameterValues(String name) {
		Map<String, String[]> m = getParameterMap();
		return m.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getServerPort()
	 */
	@Override
	public int getServerPort() {
		return _serverPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getContentType()
	 */
	@Override
	public String getContentType() {
		return HttpHeaders.getHeader(_request, HttpHeaders.Names.CONTENT_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getReader()
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(
				new ChannelBufferInputStream(_request.getContent())));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getPathInfo()
	 */
	@Override
	public String getPathInfo() {
		return null; // The only reference is in
						// MemcacheManagerServlet::getCacheType
		/*
		 * if (null == _pathInfo) { String uri = _request.getUri(); int
		 * pathInfoStart = uri.indexOf(_servletName) + _servletName.length(); //
		 * path info is specified as being decoded if (uri.indexOf('?') > 0) {
		 * // if there's a query string, omit it from the path info try {
		 * _pathInfo = URLDecoder.decode(uri.substring(pathInfoStart,
		 * uri.indexOf('?') - 1), "UTF-8"); } catch (Exception e) { // ignore }
		 * } else { try { _pathInfo =
		 * URLDecoder.decode(uri.substring(pathInfoStart), "UTF-8"); } catch
		 * (Exception e) { // ignore } } } return _pathInfo;
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getParameterMap()
	 */
	@Override
	public Map<String, String[]> getParameterMap() {
		return queryString.getParameterMap();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getContentLength()
	 */
	@Override
	public int getContentLength() {
		return (int) HttpHeaders.getContentLength(_request);
	}

	/**
	 * Returns an InputStream that can be used to read from the request.
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ServletInputStreamImpl(new ChannelBufferInputStream(
				_request.getContent()));
	}

	/**
	 * Returns the character set of the request, if one is specified in the
	 * request headers. If no character set is specified, defaults to
	 * ISO-8859-1, per RFC 2616.
	 */
	@Override
	public String getCharacterEncoding() {
		// If there is a Content-Type header present, attempt to parse out
		// the character set label.
		String temp = _request.getHeader(HttpHeaders.Names.CONTENT_TYPE);
		String[] parts = ABUtils.stringSplit(temp, ';');
		for (String part : parts) {
			String[] subparts = ABUtils.stringSplit(part, '=');
			if (subparts.length > 0 && subparts[0].equalsIgnoreCase("charset")) {
				return subparts[1].trim();
			}
		}
		// Either there was no Content-Type header or no character set label.
		// Return the default, per rfc2616.
		return DEFAULT_CHAR_ENCODING;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getLocalAddr()
	 */
	@Override
	public String getLocalAddr() {
		return _localIpAddr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.adbrite.net.http.HttpServletRequest#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(String name) {
		return _attributes.get(name);
	}

	/**
	 * Returns the server hostname, as specified in the Host http header. If no
	 * Host header was specified, returns null.
	 * Thank you Mr J for expecting "Host:" in it >:-E 
	 */
	@Override
	public String getServerName() {
		String host = HttpHeaders.getHost(_request);
		if(host!=null) {
			int idx = host.indexOf(':');
			if(idx>0) {
				host=host.substring(0, idx);
			}
		}
		return host;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getSession()
	 */
	@Override
	public HttpSession getSession() {
		throw new NotImplementedException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getParameterNames() {
		return queryString.getParameterNames();
	}

	@Override
	public String getMethod() {
		return _request.getMethod().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.adbrite.net.http.HttpServletRequest#getContextPath()
	 */
	@Override
	public String getContextPath() {
		return _contextPath;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocalPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getLocales() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRealPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getScheme() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getAuthType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getDateHeader(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServletPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public javax.servlet.http.HttpSession getSession(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public long getStartTimeMillis() {
		return startTimeMs;
	}
	public long getProcessingTimeInMillis() {
		return System.currentTimeMillis() - getStartTimeMillis();
	}

	/* (non-Javadoc)
	 * @see com.adbrite.servlet.ControllerRequest#isAuctionSimVisitor()
	 */
	@Override
	public boolean isAuctionSimVisitor(){
		InetAddress ipAddress = getRemoteIP();
		return ABUtils.isAdBriteAddress (ipAddress) && (getParameter("auctionsim") != null);
	}

	/* (non-Javadoc)
	 * @see com.adbrite.servlet.ControllerRequest#getVisitorIpAddress()
	 */
	@Override
	public InetAddress getVisitorIpAddress() {
		return getVisitorIpAddress(!isAuctionSimVisitor());
	}

	/* (non-Javadoc)
	 * @see com.adbrite.servlet.ControllerRequest#getVisitorIpAddress(boolean)
	 */
	@Override
	public InetAddress getVisitorIpAddress(boolean realVisitor) {
		
		InetAddress ipAddress = null;
		if (!realVisitor) {
			final String fakeIpAddress = getParameter(FALSIFIED_REQUESTER_ADDRESS);
			if (fakeIpAddress != null && fakeIpAddress.length() > 0) {
				try {
					ipAddress = com.google.common.net.InetAddresses.forString(fakeIpAddress);
//					LOG.info("Using fake IP address {} specified by 'abipfake' parameter. The real IP is {}",
//									new Object[] { ipAddress, InetAddress.getByName(getRemoteAddr()) });
				} catch (IllegalArgumentException e) {
//					LOG.warn("Bad IP address specified by 'abipfake' parameter {}", fakeIpAddress);
				}
			}
		} else {
			final String visitorIpAddress = getHeader(HEADER__AKAMAI_VISITOR_IP);
			if (null != visitorIpAddress) {
				//stats.numAkamaiRequests_.increment();
				try {
					ipAddress = com.google.common.net.InetAddresses.forString(visitorIpAddress.trim());
//					if (LOG.isDebugEnabled()) {
//						LOG.debug("Using visitor IP address {} specified by {} header. The real IP is {}",
//									new Object[] { ipAddress, HEADER__AKAMAI_VISITOR_IP, InetAddress.getByName(getRemoteAddr()) });
//					}
				} catch (IllegalArgumentException e) {
//					LOG.warn("Bad IP address specified by {} header: {}", HEADER__AKAMAI_VISITOR_IP, visitorIpAddress);
				}
			}
		}
		
		if (ipAddress == null) {
			ipAddress = getRemoteIP();
		}

		return ipAddress;
	}



	@Override
	public String toString() {
		return _request.toString();
	}

	@Override
	public InetAddress getLocalIP() {
		return localIP;
	}
	
	private String requestId = UUID.randomUUID().toString();
	@Override
	public String getRequestId() {
		return requestId ;
	}

}
