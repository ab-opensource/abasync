package com.adbrite.thrift;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.management.ObjectName;

import org.apache.thrift.TProcessor;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.cgbystrom.netty.thrift.ThriftPipelineFactory;
import se.cgbystrom.netty.thrift.ThriftServerHandler;

import com.adbrite.dnscache.DnsCache;
import com.adbrite.jmx.MBeanExporter;
import com.adbrite.jmx.annotations.MBean;
import com.adbrite.jmx.annotations.ManagedAttribute;

@MBean
public class ThriftServer {

	// group for listening socket channels
	static final ChannelGroup g_allChannels = new DefaultChannelGroup(
			"adserver");
	ChannelGroup channels = new DefaultChannelGroup("http");
	AtomicLong connectionsAccepted = new AtomicLong();
	AtomicLong requestsReceived = new AtomicLong();
	private static final Logger LOG = LoggerFactory.getLogger(ThriftServer.class);

	// FIXME: remove it when server is fully converted! default number of
	// threads is Ncores*2
	private int threadsNum = 50;

	private int port = 9090;
	@Resource
	private ExecutorService bossThreadPool;
	@Resource
	private ExecutorService workerThreadPool;

	private ChannelFactory factory;

	private int backlog = 150;
	private ThriftServerHandler handler;
	TProcessor processor;
	private ObjectName objectName;

	/**
	 * Start the server.
	 * 
	 * @param signal
	 */

	//@PostConstruct
	public void start() {
		handler = new NettyThriftServerHandler(this);
		
		factory = new NioServerSocketChannelFactory(bossThreadPool,
				workerThreadPool, threadsNum);
		ServerBootstrap httpbootstrap = new ServerBootstrap(factory);
		httpbootstrap.setPipelineFactory(new ThriftPipelineFactory(handler));

		httpbootstrap.setOption("backlog", getBacklog() );
		httpbootstrap.setOption("child.tcpNoDelay", true);
		httpbootstrap.setOption("child.keepAlive", true);
		httpbootstrap.setOption("child.reuseAddress", true);

		LOG.info("Starting thrift server on port " + port);
		try {
			g_allChannels.add(httpbootstrap.bind(new InetSocketAddress(port)));
		} catch (ChannelException e) {
			if("dev".equals(DnsCache.getDatacenterId())) {
				LOG.error("Ignoring binding error in dev", e);
			}
			else
				throw e;
		}
		objectName = MBeanExporter.export(this);

		LOG.info("Server startup successful.");

	}

	@PreDestroy
	public void stop() {
		if (LOG.isInfoEnabled()) {
			LOG.info("Closing all channels...");
		}
		ChannelGroupFuture future = g_allChannels.close();
		future.awaitUninterruptibly();
		if (LOG.isInfoEnabled()) {
			LOG.info("Releasing external resources...");
		}
		factory.releaseExternalResources();
		if (LOG.isInfoEnabled()) {
			LOG.info("Completed shutting down.");
		}
		MBeanExporter.unregister(objectName);
	}

	@ManagedAttribute(description="Number of currently active Thrift connections")
	public int getNumberOfChannels() {
		return channels.size();
	}

	@ManagedAttribute(description="Number of connections served since server start")
	public long getConnectionsAccepted() {
		return connectionsAccepted.get();
	}
	@ManagedAttribute
	public long getRequestsReceived() {
		return requestsReceived.get();
	}
	/**
	 * Tries to acquire the semaphore. When the semaphore is acquired, it's time
	 * for the server to exit.
	 */

	
	public void setPort(int port) {
		this.port = port;
	}

	@ManagedAttribute
	public int getPort() {
		return port;
	}

	public void setThreadsNum(int threadNum) {
		this.threadsNum = threadNum;
	}

	@ManagedAttribute
	public int getThreadNum() {
		return threadsNum;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	@ManagedAttribute
	public int getBacklog() {
		return backlog;
	}

	public TProcessor getProcessor() {
		return processor;
	}

	public void setProcessor(TProcessor processor) {
		this.processor = processor;
	}

	public ExecutorService getBossThreadPool() {
		return bossThreadPool;
	}

	public void setBossThreadPool(ExecutorService bossThreadPool) {
		this.bossThreadPool = bossThreadPool;
	}

	public ExecutorService getWorkerThreadPool() {
		return workerThreadPool;
	}

	public void setWorkerThreadPool(ExecutorService workerThreadPool) {
		this.workerThreadPool = workerThreadPool;
	}


}

