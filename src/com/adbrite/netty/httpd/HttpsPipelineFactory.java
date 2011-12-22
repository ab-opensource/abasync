package com.adbrite.netty.httpd;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.ssl.SslHandler;

public class HttpsPipelineFactory implements ChannelPipelineFactory {

	private final HttpPipelineFactory httpFactory;
	private final HttpSslContextFactory sslContextFactory;
	
	public HttpsPipelineFactory(HttpServer server) {
		httpFactory = new HttpPipelineFactory(server);
		sslContextFactory = new HttpSslContextFactory(server.getSslKeyPath());
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline p = httpFactory.getPipeline();
		SSLEngine engine = sslContextFactory.getServerContext().createSSLEngine();
		engine.setUseClientMode(false);
		p.addFirst("ssl", new SslHandler(engine));
		return p;
	}

}
