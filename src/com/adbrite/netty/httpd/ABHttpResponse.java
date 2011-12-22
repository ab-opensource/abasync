package com.adbrite.netty.httpd;

import static com.adbrite.util.QuotedStringTokenizer.quoteIfNeeded;
import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.netty.httpd.CookieEncoder.CookieDateFormat;
import com.adbrite.util.ABUtils;

public class ABHttpResponse extends DefaultHttpResponse implements HttpServletResponse {
	private static final Logger LOG = LoggerFactory
			.getLogger(ABHttpResponse.class);

	//private Map<String, Cookie> cookies = new HashMap<String, Cookie>();
	private PrintWriter _writer = null;
	private ChannelBufferOutputStream _ostream;
	private ServletOutputStreamImpl _sostream = null;

	public ABHttpResponse(HttpResponseStatus status) {
		super(HttpVersion.HTTP_1_1,status);
		resetBuffer();
	}
	
	/**
	 * Create default response with status 200
	 */
	public ABHttpResponse() {
		this(HttpResponseStatus.OK);
	}

	public ABHttpResponse(String contentType) {
		this(HttpResponseStatus.OK);
		setContentType(contentType);
	}
	public ABHttpResponse(String contentType, CharSequence content) {
		this(HttpResponseStatus.OK);
		setContentType(contentType);
		setContent(content);
	}
	public ABHttpResponse(String contentType, byte[] content) {
		this(HttpResponseStatus.OK);
		setContentType(contentType);
		setContent(content);
	}

	@Override
	public void addCookie(Cookie cookie) {
		addCookieHeader(cookie);
	}

	@Override
	public void setContentType(String type) {
		setHeader(HttpHeaders.Names.CONTENT_TYPE, type);
	}

	@Override
	public void setContentLength(int len) {
		// We deliberately ignore this value and set the content length from the
		// content itself
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (null == _writer) {
			_writer = new ABPrintWriter(getChannelBufferOutputStream());
		}
		return _writer;
	}

	@Override
	public void setHeader(String name, String value) {
		setHeader(name, (Object) value);
	}

	@Override
	public void addHeader(String name, String value) {
		addHeader(name, (Object) value);
	}

	@Override
	public void sendRedirect(String location) {
		setHeader(HttpHeaders.Names.LOCATION, location);
		setStatus(HttpResponseStatus.MOVED_PERMANENTLY);
	}

	@Override
	public void setStatus(int sc) {
		setStatus(HttpResponseStatus.valueOf(sc));
	}

	@Override
	public void sendError(int sc) throws IOException {
		setStatus(HttpResponseStatus.valueOf(sc));
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		setStatus(HttpResponseStatus.valueOf(sc));
		setContent(copiedBuffer(msg, CharsetUtil.UTF_8));
	}

	@Override
	public ServletOutputStream getOutputStream() {
		if (null == _sostream) {
			_sostream = new ServletOutputStreamImpl(
					getChannelBufferOutputStream());
		}
		return _sostream;
	}

	public ChannelBufferOutputStream getChannelBufferOutputStream() {
		return _ostream;
	}

	public void sendError(HttpResponseStatus status, String msg)
			throws IOException {
		setStatus(status);
		setContent(copiedBuffer(msg, CharsetUtil.UTF_8));
		_sostream = null; // nothing should be added after this call
	}

	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void flushBuffer() {
	}

	private CookieEncoder.CookieDateFormat cookieDateFormat = new CookieDateFormat();

	private void addCookieHeader(Cookie cookie) {
			//encode the cookie
            if(LOG.isDebugEnabled()) {
            	LOG.debug("encoding cookie "+cookie.getName());
            }
            String name = cookie.getName();
            String value = cookie.getValue();
            int version = cookie.getVersion();
            
            // Check arguments
            if (name == null || name.length() == 0) {
            	LOG.info("Illegal cookie name "+name);
            	return;
            }
            StringBuilder sb = new StringBuilder(128+(value!=null?value.length():0));
            quoteIfNeeded(sb, name);
            sb.append('=');
            if(value!=null && value.length()>0)
            	quoteIfNeeded(sb, value);
            if(version>0) {
            	sb.append("; version=");
            	sb.append(version);
            	//comment goes here, but we don't use it
            }
            String path = cookie.getPath();
            if(path!=null) {
            	sb.append("; path=");
            	quoteIfNeeded(sb, path);
            }

            String domain = cookie.getDomain();
            if(domain!=null && domain.length()>0) {
            	sb.append("; domain=");
            	quoteIfNeeded(sb, domain);
            }
            long maxAge = cookie.getMaxAge();
            if(maxAge>=0) {
            	if(version==0) {
            		sb.append("; expires=");
            		sb.append(cookieDateFormat.format(
                            new Date(System.currentTimeMillis() +
                                     maxAge * 1000L)));
            	} else {
            		sb.append("; max-age=");
            		sb.append(maxAge);
            	}
            } else if(version>0) {
            	sb.append("; discard");
            }
            if (cookie.getSecure())
            {
                sb.append("; secure");
            }

			addHeader(HttpHeaders.Names.SET_COOKIE, sb);
		}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public String getContentType() {
		return getHeader(HttpHeaders.Names.CONTENT_TYPE);
	}

	@Override
	public Locale getLocale() {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public void reset() {
		resetBuffer();
	}

	@Override
	public void resetBuffer() {
		ChannelBuffer buffer = dynamicBuffer(4096);
		setContent(buffer);
		_ostream = new ChannelBufferOutputStream(buffer);
	}

	@Override
	public void setBufferSize(int arg0) {
	}

	@Override
	public void setCharacterEncoding(String arg0) {
	}

	@Override
	public void setLocale(Locale arg0) {
	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public boolean containsHeader(String arg0) {
		return null != getHeader(arg0);
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public String encodeURL(String arg0) {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public String encodeUrl(String arg0) {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		throw new NotImplementedException("Method is not implemented");
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		setStatus(HttpResponseStatus.valueOf(arg0));
	}

	public void setContent(byte[] content) {
		setContent(ChannelBuffers.wrappedBuffer(content));
	}
	public void setContent(CharSequence content) {
		setContent(ChannelBuffers.copiedBuffer(content, ABUtils.UTF8));
	}
}
