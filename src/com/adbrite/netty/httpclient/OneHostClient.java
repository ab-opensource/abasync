package com.adbrite.netty.httpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelUpstreamHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.counters.CircularBuffer;
import com.adbrite.counters.CounterEMA;
import com.adbrite.dnscache.DnsCache;
import com.adbrite.jmx.MBeanExporter;
import com.adbrite.jmx.annotations.ManagedAttribute;
import com.adbrite.netty.AsyncCallback;
import com.adbrite.netty.NettyPersistentConnection;
import com.adbrite.netty.OpExecutionStats;
import com.adbrite.util.ABUtils;

public class OneHostClient extends IdleStateAwareChannelUpstreamHandler implements HttpClient, OpExecutionStats{

	private static final Logger LOG = LoggerFactory.getLogger(OneHostClient.class);

	@ManagedAttribute(writable=true)
	private String host;

	@ManagedAttribute(writable=true)
	private int port;

	private final ClientBootstrap bootstrap;
	
	@ManagedAttribute(writable=true)
	private volatile boolean stopped;

	protected final LinkedBlockingDeque<Channel> readyChannels = new LinkedBlockingDeque<Channel>();

	private final ChannelGroup allChannels = new DefaultChannelGroup();

	private final AtomicLong timesConnectionFailure = new AtomicLong();
	@ManagedAttribute
	public long getTimesConnectionFailure() {
		return timesConnectionFailure.get();
	}
	private final AtomicLong lastRequestTimeNs = new AtomicLong(System.nanoTime());
	// Exponential Moving Average of time between requests
	private final CounterEMA avgWaitingNs = new CounterEMA();
	@ManagedAttribute
	public double getAvgRequestsPerSecond() {
		return ((double) TimeUnit.SECONDS.toNanos(1))
				/ (avgWaitingNs.get());
	}

	@SuppressWarnings("unused")
	@ManagedAttribute(writable=false)
	private volatile String lastFailureReason = null;
	
	/*
	 * connection quality score accumulator
	 * 0=perfect, 100=poor
	 * we are using high EMA weight to make it faster
	 */
	private final CounterEMA failureRate = new CounterEMA(0.1);

	private final ChannelFutureListener ON_CONNECTION = new ChannelFutureListener() {
		public void operationComplete(ChannelFuture future) {
			if (future.isSuccess()) {
				Channel channel = future.getChannel();
				if(stopped) {
					channel.close();
					return;
				}
				if(LOG.isDebugEnabled())
					LOG.debug("http connected to "+channel.getRemoteAddress());
				HttpOperation op = (HttpOperation) future.getChannel().getPipeline().getContext(OneHostClient.this).getAttachment();
				if(trace) {
					LOG.info("channel "+channel+" connected, op="+op);
				}

				if(op!=null && !op.isFinished())
					execute(op, channel); //already linked channel to op
				else {
					future.getChannel().getPipeline().getContext(OneHostClient.this).setAttachment(null);
					op.setChannel(null);
					readyChannels.push(channel);
				}

			} else {
				timesConnectionFailure.incrementAndGet();
				future.getChannel().close();
				lastFailureReason = future.getCause().getMessage();
				//LOG.info("Unable to connect",future.getCause().getMessage());
				HttpOperation op = (HttpOperation) future.getChannel().getPipeline().getContext(OneHostClient.this).getAttachment();
				if(op!=null) {
					if(op.shouldRetry()) {
						connect(op);
					} else {
						if(op!=null)
							op.notifyListener(future.getCause());
					}
				}
			}
		}
	};

	private CircularBuffer latencyValues = new CircularBuffer();
	@ManagedAttribute
	public long[] getLatencyValues() {
		return latencyValues.getAll();
	}
	protected final CounterEMA avgLatencyNs = new CounterEMA();
	@ManagedAttribute
	public double getAvgLatencyMs() {
		return avgLatencyNs.get()/ABUtils.NANOSECONDS_IN_A_MILLISECOND ;
	}
	@ManagedAttribute(description="Number of operations queued for execution")
	private final AtomicLong opsQueued = new AtomicLong();
	@ManagedAttribute(description="Number of executions including retries")
	private final AtomicLong opsExecuted = new AtomicLong();
	@ManagedAttribute(description="Number of failed operations")
	private final AtomicLong opsFailed = new AtomicLong();
	@ManagedAttribute(description="Number of succesful operations")
	private final AtomicLong opsSucceeded = new AtomicLong();

	@ManagedAttribute(description="Keepalive support detected", writable=true)
	private volatile boolean canKeepAlive=false;

	@ManagedAttribute(description="Keepalive detection override", writable=true)
	private boolean canKeepAliveOverride=false;

	private final ObjectName objectName;
	
	private final boolean trace = false;

	public OneHostClient(String host, int port,
			Executor bossExecutor, Executor workerExecutor) throws UnknownHostException {
		this(host, port, bossExecutor, workerExecutor, 3);
	}
	public OneHostClient(String host, int port,
			Executor bossExecutor, Executor workerExecutor, int retryLimit) throws UnknownHostException {
		this.host = host;
		this.port = port;
		this.retryLimit=retryLimit;
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(bossExecutor, workerExecutor));
		
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
		        ChannelPipeline pipeline = Channels.pipeline();
		        pipeline.addLast("idle", new IdleStateHandler(NettyPersistentConnection.timer, 60, 0, 0));
		        pipeline.addLast("codec", new HttpClientCodec(4096, 328768, 1024*1024 ));
		        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
				pipeline.addLast("handler", OneHostClient.this);
				return pipeline;
			}
		});
		bootstrap.setOption("connectTimeoutMillis", 1000);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("child.reuseAddress", true);
		bootstrap.setOption("child.soLinger", false);
		bootstrap.setOption("child.receiveBufferSize", 128 * 1024);
		bootstrap.setOption("child.sendBufferSize", 128 * 1024);
		objectName = MBeanExporter.export(this, "com.adbrite.netty.httpclient:type=FixedOneHostClient,addr="+host+"-"+port);
	}
	
	@Override
	public void setAddress(String host, int port) {
		this.host=host;
		this.port=port;
	}
	private final AtomicLong idx = new AtomicLong();

	private int retryLimit;

	@SuppressWarnings("unused")
	@ManagedAttribute(writable=false)
	private InetAddress lastIp;
	
	private void connect(HttpOperation op) {
		try {
			InetAddress[] ips = DnsCache.getAllByName(host);
			if(ips.length==0) {
				throw new UnknownHostException("hostname "+host+" resolved to 0 ips");
			}
			InetAddress ip = ips[(int) (idx.incrementAndGet()%ips.length)];
			lastIp = ip;
			InetSocketAddress address = new InetSocketAddress(ip, port);
			ChannelFuture f = bootstrap.connect(address);
			Channel channel = f.getChannel();
			allChannels.add(channel);
			op.setChannel(channel);
			channel.getPipeline().getContext(this).setAttachment(op);
			f.addListener(ON_CONNECTION);
		} catch (UnknownHostException e) {
			LOG.error("Unable to connect", e.getMessage());
			if(op!=null)
				op.notifyListener(e);
		}
	}

	@Override
	public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
			throws Exception {
		if(e.getState()==IdleState.READER_IDLE) {
			if(LOG.isDebugEnabled())
				LOG.debug("http read timeout "+e.getChannel().getRemoteAddress());
			e.getChannel().close();
		}
	}
	//We are not using channelConnected to avoid race condition on setting op pointer in the context
	@Override
	public void channelConnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		super.channelConnected(ctx, e);
	}
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		Channel channel = e.getChannel();
		readyChannels.remove(channel);
        HttpOperation op = (HttpOperation) ctx.getAttachment();
        if(op!=null) {
        	ctx.setAttachment(null);
        	op.setChannel(null);
        	if(!op.isFinished()) {
        		if(op.shouldRetry())
        			execute(op);
				else
					if(op!=null)
						op.notifyListener(new IOException("Disconnected"));
        	}
        }
		super.channelDisconnected(ctx, e);
	}
	
	private void execute(HttpOperation op, Channel channel) {
		if(trace) {
			LOG.info("executing op "+op+" on channel "+channel);
		}
		try {
			long now = System.nanoTime();
			while(true) {
				long was = lastRequestTimeNs.get();
				if(!lastRequestTimeNs.compareAndSet(was, now))
					continue;
				avgWaitingNs.add(now-was);
				break;
			}
			HttpRequest buf = op.resetAndBuildRequest();
			if(LOG.isDebugEnabled()) {
				LOG.debug(channel.getRemoteAddress().toString()+"<--"+buf.toString());
			}
			channel.write(buf);
			opsExecuted.incrementAndGet();
		} catch (Exception e) {
			if(op!=null)
				op.notifyListener(e);
		}
	}
	
	public boolean execute(HttpOperation op) {
		if(stopped)
			return false;
		op.setStatsCallback(this);
		if(trace)
			LOG.info("execute "+op);
		opsQueued.incrementAndGet();
		Channel channel = readyChannels.pollFirst();
		if(channel!=null) {
			if(trace)
				LOG.info("execute "+op+" on keeplive channel "+channel);
			op.setChannel(channel);
			channel.getPipeline().getContext(this).setAttachment(op);
			execute(op, channel);
		} else {
			connect(op);
		}
		return true;
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		Throwable cause = e.getCause();
		if( cause instanceof ClosedChannelException
				|| cause instanceof java.net.ConnectException
				) {
			return;
		}
		Channel channel = e.getChannel();
		HttpOperation op = (HttpOperation) ctx.getAttachment();
		if(op!=null) {
			op.setChannel(null);
		}
		ctx.setAttachment(null);
		if(channel.isOpen())
			channel.close();
		//DO NOT TRY TO RECOVER
		if(op!=null)
			op.notifyListener(cause);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Channel channel = e.getChannel();
		Object message = e.getMessage();
		if (message instanceof HttpResponse) {
			HttpResponse resp = (HttpResponse) message;
	        HttpOperation op = (HttpOperation) ctx.getAttachment();
	        if(op==null) {
	        	LOG.debug("Response without request");
	        	channel.close();
	        	return;
	        }
			if(trace) {
				LOG.info("message received on channel "+channel+" associated op "+op+" resp id "+resp.getHeader(HttpOperation.X_ADBRITE_REQUEST_ID));
			}
	        ctx.setAttachment(null);
	        op.setChannel(null);
			if(LOG.isDebugEnabled()) {
				LOG.debug(e.getRemoteAddress().toString()+"-->"+resp.toString());
			}
			op.readFromBuffer(resp);
			boolean canKeep = HttpHeaders.isKeepAlive(resp);
			if(canKeepAliveOverride) {
				canKeep = canKeepAlive;
			}
			else {
				canKeepAlive= canKeep;
			}
			if(canKeep) {
				readyChannels.push(channel);
			} else {
				channel.close();
			}
			long executionTime = op.getExecutionTime();
			avgLatencyNs.add(executionTime);
			latencyValues.put(executionTime);
			if(!op.isFinished())
				op.notifyListener();
		}

	}
	
	@Override
	public void execute(HttpRequest request, int timeoutInMs,
			final AsyncCallback<HttpResponse> callback) {
		execute(new HttpOperation(request, timeoutInMs, retryLimit) {
			@Override
			public void operationCompleted() {
				if(isSucess()) {
					callback.notifyListener(getResponse());
				} else {
					callback.notifyListener(getFailureReason());
				}
			}
		});
	}
	
	
	@ManagedAttribute
	public int getConnections() {
		return allChannels.size();
	}
	
	@ManagedAttribute
	public int getConnectionsReady() {
		return readyChannels.size();
	}
	

	@Override
	public void stop() {
		stopped = true;
		allChannels.close();
		MBeanExporter.unregister(objectName);
	}

	@Override
	@ManagedAttribute
	public boolean isAvailable() {
		//TODO: do something more meaningful
		return true;
	}

	@ManagedAttribute
	public int getRetryLimit() {
		return retryLimit;
	}

	@ManagedAttribute
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}
	
	@Override
	@ManagedAttribute(description="error rate in percent, 0 = perfect connection")
	public double getFailureRate() {
		return failureRate.get();
	}

	@Override
	public void registerSuccess(long executionTime) {
		failureRate.add(0.);
		avgLatencyNs.add(executionTime);
		latencyValues.put(executionTime);
		opsSucceeded.incrementAndGet();
	}
	@Override
	public void registerFailure(long executionTime, Throwable e) {
		failureRate.add(100.);
		avgLatencyNs.add(executionTime);
		latencyValues.put(executionTime);
		lastFailureReason = e.getMessage();
		opsFailed.incrementAndGet();
		lastFailureReason = e.getMessage();
	}
	
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
			throws Exception {
		if(trace) {
			Channel channel = ctx.getChannel();
			HttpOperation op = (HttpOperation) ctx.getAttachment();
			LOG.info("handleUpstream channel "+channel+" op "+op+" event "+e);
		}
		super.handleUpstream(ctx, e);
	}
	
//	public void setTrace(boolean trace) {
//		this.trace = trace;
//	}

}
