package com.adbrite.netty.httpd;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class SimpleServletConfig implements ServletConfig {

	private ServletContext context;
	
	public SimpleServletConfig(ServletContext context) {
		this.context = context;
	}

	@Override
	public String getInitParameter(String arg0) {
		return context.getInitParameter(arg0);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getInitParameterNames() {
		return context.getInitParameterNames();
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getServletName() {
		return "ServletName";
	}

}
