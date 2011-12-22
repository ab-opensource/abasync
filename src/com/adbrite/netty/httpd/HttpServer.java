package com.adbrite.netty.httpd;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
//import org.springframework.jmx.export.annotation.ManagedAttribute;
//import org.springframework.jmx.export.annotation.ManagedOperation;
//import org.springframework.jmx.export.annotation.ManagedResource;

import com.adbrite.dnscache.DnsCache;
import com.adbrite.jmx.MBeanExporter;
import com.adbrite.jmx.annotations.MBean;
import com.adbrite.jmx.annotations.ManagedAttribute;
import com.adbrite.jmx.annotations.ManagedOperation;

@MBean
public class HttpServer {
	// group for listening socket channels
	static final ChannelGroup g_allChannels = new DefaultChannelGroup(
			"adserver");
	ChannelGroup httpChannels = new DefaultChannelGroup("http");
	AtomicLong connectionsAccepted = new AtomicLong();
	AtomicLong requestsReceived = new AtomicLong();
	private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

	// FIXME: remove it when server is fully converted! default number of
	// threads is Ncores*2
	private int threadsNum = 150;

	private int[] ports = {80, 8080};
	private int[] sslPorts = {443, 8443};
	@Resource
	private ExecutorService bossThreadPool;
	@Resource
	private ExecutorService workerThreadPool;
	private HttpDispatchTable dispatchTable;
	private ChannelFactory factory;

	@Resource
	HttpLogger logger;
	private boolean enableAccessLog = false;
	private int backlog = 150;
	private boolean enableKeepAlive = false;
	private String sslKeyPath = "/opt/ssl-certificates/adbrite/wildcard_adbrite.p12"; //default

	/**
	 * Start the server.
	 * 
	 * @param signal
	 */

	@PostConstruct
	public void start() {
		factory = new NioServerSocketChannelFactory(bossThreadPool,
				workerThreadPool, threadsNum);
		ServerBootstrap httpbootstrap = new ServerBootstrap(factory);
		httpbootstrap.setPipelineFactory(new HttpPipelineFactory(this));

		httpbootstrap.setOption("backlog", getBacklog() );
		httpbootstrap.setOption("child.tcpNoDelay", true);
		httpbootstrap.setOption("child.keepAlive", true);
		httpbootstrap.setOption("child.reuseAddress", true);

		int firstport = 0;
		for (int port: ports) {
			LOG.info("Starting http server on port " + port);
			try {
				g_allChannels.add(httpbootstrap.bind(new InetSocketAddress(port)));
				if(firstport==0)
					firstport=port;
			} catch (ChannelException e) {
				if("dev".equals(DnsCache.getDatacenterId())) {
					LOG.error("Ignoring binding error in dev", e);
				}
				else
					throw e;
			}
		}

		ServerBootstrap httpsbootstrap = new ServerBootstrap(factory);
		httpsbootstrap.setPipelineFactory(new HttpsPipelineFactory(this));

		httpsbootstrap.setOption("backlog", getBacklog() );
		httpsbootstrap.setOption("child.tcpNoDelay", true);
		httpsbootstrap.setOption("child.keepAlive", true);
		httpsbootstrap.setOption("child.reuseAddress", true);

		for (int port: sslPorts) {
			LOG.info("Starting https server on port " + port);
			try {
				g_allChannels.add(httpsbootstrap.bind(new InetSocketAddress(port)));
			} catch (ChannelException e) {
				if("dev".equals(DnsCache.getDatacenterId())) {
					LOG.error("Ignoring binding error in dev", e);
				}
				else
					throw e;
			}
		}
		String mbeanName =  "com.adbrite.netty.httpd:type=HttpServer,port="+firstport;
		MBeanExporter.export(this, mbeanName);
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

	}

	@ManagedAttribute(description="Number of currently active HTTP(S) connections")
	public int getNumberOfHttpChannels() {
		return httpChannels.size();
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

	public void setPorts(int[] ports) {
		this.ports = ports;
	}

	public int[] getPorts() {
		return ports;
	}

	public void setSslPorts(int[] sslPorts) {
		this.sslPorts = sslPorts;
	}

	public int[] getSslPorts() {
		return sslPorts;
	}

	public void setThreadsNum(int threadNum) {
		this.threadsNum = threadNum;
	}

	public int getThreadNum() {
		return threadsNum;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	public int getBacklog() {
		return backlog;
	}

	@ManagedOperation
	public void enableAccessLog() {
		this.enableAccessLog = true;
	}

	@ManagedOperation
	public void disableAccessLog() {
		this.enableAccessLog = false;
	}

	public void setEnableAccessLog(boolean enableAccessLog) {
		this.enableAccessLog = enableAccessLog;
	}

	@ManagedAttribute
	public boolean isEnableAccessLog() {
		return enableAccessLog;
	}

	@ManagedOperation
	public void enableKeepAlive() {
		enableKeepAlive=true;
	}

	@ManagedOperation
	public void disableKeepAlive() {
		enableKeepAlive=false;
	}

	public void setEnableKeepAlive(boolean enableKeepAlive) {
		this.enableKeepAlive = enableKeepAlive;
	}

	@ManagedAttribute
	public boolean isEnableKeepAlive() {
		return enableKeepAlive;
	}

	public String getSslKeyPath() {
		return sslKeyPath ;
	}
	public void setSslKeyPath(String sslKeyPath) {
		this.sslKeyPath = sslKeyPath;
	}

	public HttpDispatchTable getDispatchTable() {
		return dispatchTable;
	}

	@Required
	public void setDispatchTable(HttpDispatchTable dispatchTable) {
		this.dispatchTable = dispatchTable;
	}

}
