package com.adbrite.netty.httpd;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleServletContext implements ServletContext {
	private static final Logger LOG = LoggerFactory
	.getLogger(HttpServer.class);

	private Map<String, Object> attributes = new HashMap<String, Object>();
	private Map<String, String> initParams = new HashMap<String, String>();
	{
		initParams.put("contextClass", "com.adbrite.springframework.AdbriteXmlWebApplicationContext");
		//initParams.put("parentContextKey", "WebContent/WEB-INF/applicationContext.xml");
		initParams.put("contextConfigLocation", "WebContent/WEB-INF/applicationContext.xml");
	}

	@Override
	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getAttributeNames() {
		return new IteratorEnumeration(attributes.keySet().iterator());
	}

	@Override
	public ServletContext getContext(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getContextPath() {
		return "/";
	}

	@Override
	public String getInitParameter(String arg0) {
		return initParams.get(arg0);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getInitParameterNames() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMajorVersion() {
		return 2;
	}

	@Override
	public String getMimeType(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMinorVersion() {
		throw new UnsupportedOperationException();
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRealPath(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public URL getResource(String resource)
			throws MalformedURLException {
		if (resource.startsWith("/"))
			resource = resource.substring(1);

		File file = new File(resource);

		if (file.exists())
			return new URL("file://" + resource);
		else
			return null;
	}

	@Override
	public InputStream getResourceAsStream(String resource) {
		if (resource.startsWith("/"))
			resource = resource.substring(1);

		File file = new File(resource);
		InputStream is;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("could not open "
					+ file.getAbsolutePath(), e);
		}

		assertNotNull("getResourceAStream: " + resource, is);

		return is;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Set getResourcePaths(String arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getServerInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Servlet getServlet(String arg0) throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getServletContextName() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getServletNames() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getServlets() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void log(String arg0) {
		LOG.info(arg0);
	}

	@Override
	public void log(Exception arg0, String arg1) {
		LOG.error(arg1, arg0);
	}

	@Override
	public void log(String arg0, Throwable arg1) {
		LOG.error(arg0, arg1);
	}

	@Override
	public void removeAttribute(String arg0) {
		attributes.remove(arg0);
	}

	@Override
	public void setAttribute(String key, Object value) {
		attributes .put(key, value);
	}

}
