package com.adbrite.netty.httpd;

import static org.jboss.netty.channel.Channels.pipeline;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;

public class HttpPipelineFactory implements ChannelPipelineFactory {
	private static final int COMPRESSION_LEVEL = 6;
	private static final int MAX_CONTENT_LENGTH = 128*1024; //total limit of chunked REQUEST length
	private static final int MAX_CHUNK_SIZE = 50*1024; //REQUEST max chunk size
	private static final int MAX_HEADER_SIZE = 50*1024;
	private static final int MAX_INITIAL_LINE_LENGTH = 60*1024;
	private static final IdleConnectionKiller HANDLER_IDLEKILLER = new IdleConnectionKiller();
	HttpServer httpServer;
	Timer timer = new HashedWheelTimer(1, TimeUnit.SECONDS) {
		@Override
		public Set<Timeout> stop() {
			throw new IllegalStateException("Attempt to close the HTTP timer");
		}
	};

	public HttpPipelineFactory(HttpServer httpServer) {
		this.httpServer = httpServer;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline p = pipeline();
		p.addLast("idlehandler", new IdleStateHandler(timer, 20, 20, 0));
		p.addLast("idlekiller", HANDLER_IDLEKILLER);
		p.addLast("decoder", new HttpRequestDecoder(MAX_INITIAL_LINE_LENGTH, MAX_HEADER_SIZE, MAX_CHUNK_SIZE));
		p.addLast("encoder", new HttpResponseEncoder());
		p.addLast("decompressor", new HttpContentDecompressor());
		p.addLast("aggregator", new HttpChunkAggregator(MAX_CONTENT_LENGTH));
		p.addLast("compressor", new HttpContentCompressor(COMPRESSION_LEVEL));
		p.addLast("resheaders", new HttpResponceHeadersManager(httpServer.isEnableKeepAlive()));
		if(httpServer.isEnableAccessLog())
			p.addLast("logger", new HttpLoggerHandler(httpServer.logger, httpServer.isEnableAccessLog()));
		p.addLast("dispatcher", new HttpDispatchHandler(httpServer));
		return p;
	}

}
