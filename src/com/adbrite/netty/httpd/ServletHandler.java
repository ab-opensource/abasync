package com.adbrite.netty.httpd;

import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletHandler extends SimpleChannelUpstreamHandler {
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpDispatchHandler.class);

	private Executor workerExecutor;

	private GenericServlet servlet;
	
	private static ServletContext sc;
	public static void setServletContext(ServletContext context) {
		sc = context;
	}

	public ServletHandler() {
		
	}
	
	public ServletHandler(GenericServlet obj, Executor workerExecutor) {
		servlet = obj;
		this.workerExecutor = workerExecutor;
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent evt)
			throws Exception {
		HttpRequest _request = (HttpRequest) evt.getMessage();
		final ABHttpRequest req = new ABHttpRequest(_request,
				evt.getChannel());
		final Channel channel = evt.getChannel();
		workerExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				HttpResponse r;
				ABHttpResponse resp = new ABHttpResponse();
				try {
					servlet.service(req, resp);
					r=resp;
				} catch (Throwable e) {
					LOG.error("Exception while calling servlet", e);
					r = new HttpResponse500();
				}
				channel.write(r);
			}
		});

	}

	public void setServlet(GenericServlet servlet) {
		this.servlet = servlet;
	}
	
//	I don't know whether servlet config can be shared or not
//	This approach stinks but...
	@PostConstruct
	public void init() throws ServletException {
		final ServletConfig servletConfig = new SimpleServletConfig(sc);
		servlet.init(servletConfig);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		LOG.error("ServletHandler exception", e);
		Channel channel = e.getChannel();
		if(channel.isConnected())
			channel.close();
	}

}
