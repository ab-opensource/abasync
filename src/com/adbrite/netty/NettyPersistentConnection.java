package com.adbrite.netty;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;

import com.adbrite.counters.CounterEMA;
import com.adbrite.dnscache.DnsCache;
import com.adbrite.jmx.MBeanExporter;
import com.adbrite.jmx.annotations.ManagedAttribute;
import com.adbrite.jmx.annotations.ManagedOperation;
import com.adbrite.util.ABUtils;

public abstract class NettyPersistentConnection<OP extends NetworkOperation<?,?>> extends SimpleChannelUpstreamHandler implements OpExecutionStats {
	private final Logger LOG = getLog();
	abstract protected Logger getLog();
	protected int maxOps = 1000;

	private static final int MAX_RECONNECTION_TIMEOUT = 5000;
	private int firstReconnectionTimeout = 500;
	
	private static enum State {
		INITIAL, //Initial state, run start() to begin accepting ops and connecting. can get there after closing if not keepalive
		CONNECTING, //trying to connect
		RECONNECTION_TIMEOUT, //tried tonnecting, failed, now the connection is timing out for another try
		CONNECTED, //normal connected state
		CLOSING, //client-side initiated closing
	}
	
	protected volatile State connectionState = State.INITIAL;
	private ChannelGroup channelGroup;
	
	@ManagedAttribute
	public String getConnectionState() {
		return connectionState.toString();
	}

	private CounterEMA avgLatencyNs = new CounterEMA();
	@ManagedAttribute
	public double getAvgLatencyMs() {
		return avgLatencyNs.get()/ABUtils.NANOSECONDS_IN_A_MILLISECOND ;
	}

	/**
	 * This client is trying to reconnect immediately if connection lost.
	 * If the first reconnection fails this is the first timeout before the second attempt.
	 * @param firstReconnectionTimeoutMillis timeout in milliseconds
	 */
	public void setFirstReconnectionTimeout(int firstReconnectionTimeoutMillis) {
		this.firstReconnectionTimeout = firstReconnectionTimeoutMillis;
	}
	//right, we are using this everywhere.
	public static Timer timer = new HashedWheelTimer(
			50, TimeUnit.MILLISECONDS);
	private String host;
	private InetSocketAddress address;
	private int port;
	private final ClientBootstrap bootstrap;
	
    /**
     *  Create a pipeline without the last handler (it will be added right before connecting).
     * @return
     */

	protected abstract ChannelPipeline getPipeline();
	
	private ChannelPipelineFactory cpf = new ChannelPipelineFactory() {
		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = NettyPersistentConnection.this.getPipeline();
			pipeline.addLast("handler", NettyPersistentConnection.this);
			return pipeline;
		}
	};
	private ObjectName objectName;

	public NettyPersistentConnection(InetAddress ip, int port, Executor bossExecutor, Executor workerExecutor, String mbeanName) throws UnknownHostException {
		this(new InetSocketAddress( ip, port), bossExecutor, workerExecutor, mbeanName);
	}	

	public NettyPersistentConnection(String host, int port, Executor bossExecutor, Executor workerExecutor, String mbeanName) throws UnknownHostException {
		this(new InetSocketAddress( DnsCache.getByName(host), port), bossExecutor, workerExecutor, mbeanName);
		this.host = host;
	}	
	/**
	 * Create new connection. DOES NOT INITIATE CONNECTION, use start()!
	 * @param address
	 * @param bossExecutor
	 * @param workerExecutor
	 */

	public NettyPersistentConnection(InetSocketAddress address, Executor bossExecutor, Executor workerExecutor, String mbeanname) {
		this.address = address;
		this.port = address.getPort();
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(bossExecutor, workerExecutor));
		bootstrap.setPipelineFactory(cpf);
		bootstrap.setOption("connectTimeoutMillis", 1000);
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.setOption("child.reuseAddress", true);
		bootstrap.setOption("child.soLinger", false);
		bootstrap.setOption("child.receiveBufferSize", 128 * 1024);
		bootstrap.setOption("child.sendBufferSize", 128 * 1024);
		if(mbeanname!=null) {
			objectName = MBeanExporter.export(this, mbeanname);
		}
	}

	private AtomicLong timesConnected = new AtomicLong();
	@ManagedAttribute
	public long getTimesConnected() {
		return timesConnected.get();
	}
	private AtomicLong timesDisonnected = new AtomicLong();
	@ManagedAttribute
	public long getTimesDisonnected() {
		return timesDisonnected.get();
	}
	private AtomicLong timesConnectionFailure = new AtomicLong();
	@ManagedAttribute
	public long getTimesConnectionFailure() {
		return timesConnectionFailure.get();
	}
	private AtomicLong opsQueued = new AtomicLong();
	@ManagedAttribute
	public long getOpsQueued() {
		return opsQueued.get();
	}
	private AtomicLong opsDropped = new AtomicLong();
	@ManagedAttribute
	public long getOpsDropped() {
		return opsDropped.get();
	}
	private AtomicLong opsFailed = new AtomicLong();
	@ManagedAttribute
	public long getOpsFailed() {
		return opsFailed.get();
	}
	private AtomicLong bytesSent = new AtomicLong();
	@ManagedAttribute
	public long getBytesSent() {
		return bytesSent.get();
	}
	private String lastFailureReason = null;
	@ManagedAttribute
	public String getLastFailureReason() {
		return lastFailureReason;
	}
	private volatile long lastSuccessTimestamp = 0;
	public long getLastSuccessTimestamp() {
		return lastSuccessTimestamp;
	}
	/*
	 * connection quality score accumulator
	 * 0=perfect, 100=poor
	 * we are using high EMA weight to make it faster
	 */
	protected final CounterEMA failureRate = new CounterEMA(0.1);

	public void registerSuccess(long executionTime) {
		lastSuccessTimestamp = System.currentTimeMillis();
		failureRate.add(0.);
		avgLatencyNs.add(executionTime);
	}
	public void registerFailure(long executionTime, Throwable failureReason) {
		avgLatencyNs.add(executionTime);
		lastFailureReason = failureReason.getMessage();
	}

	
	protected volatile Channel channel = null;
	private ChannelFutureListener ON_CONNECTION_FAILURE = new ChannelFutureListener() {
		public void operationComplete(ChannelFuture future) {
			if (!future.isSuccess()) {
				timesConnectionFailure.incrementAndGet();
				onConnectionFailure();
				dropInQueue(future.getCause());
				future.getChannel().close();
				// LOG.info("Unable to connect to "+address);
				rescheduleConnection();
			}
		}
	};
	
	private volatile int timeoutMillis=10;

	private final TimerTask reconnectionTask = new TimerTask() {
		@Override
		public void run(Timeout timeout) throws Exception {
			connect();
		}
	};
	
	private void rescheduleConnection() {
		if(stopped) {
			if(LOG.isDebugEnabled())
				LOG.debug(address.toString()+" client is stopped, cancel rescheduling connection to "+address+" in "+timeoutMillis+" ms");
			return;
		}
		if(LOG.isDebugEnabled())
			LOG.debug(address.toString()+" scheduling connection to "+address+" in "+timeoutMillis+" ms");
		timer.newTimeout(reconnectionTask , timeoutMillis, TimeUnit.MILLISECONDS);
		timeoutMillis = timeoutMillis + timeoutMillis/5;
		if(timeoutMillis>MAX_RECONNECTION_TIMEOUT)
			timeoutMillis=MAX_RECONNECTION_TIMEOUT;
		connectionState = State.RECONNECTION_TIMEOUT;
	}
	
	ChannelFuture connect() {
		if(stopped) {
			return null;
		}
		connectionState = State.CONNECTING;
		if(host!=null) {
			try {
				InetAddress ip = DnsCache.getByName(host);
				address = new InetSocketAddress(ip, port);
			} catch (UnknownHostException e) {
				LOG.warn("Unable to resolve ip address for "+host+", using the old record");
			}
		}
		ChannelFuture f = bootstrap.connect(address);
		f.addListener(ON_CONNECTION_FAILURE);
		return f;
	}
	private volatile boolean stopped = false;

	protected final LinkedBlockingDeque<OP> opq = new LinkedBlockingDeque<OP>();

	/**
	 * Reset the state of decoder.
	 * For example, memcached client clear its buffer.
	 * we can skip this part if use pipeline decoders only
	 */
	protected void initDecoder() {
	}
	@Override
	public void channelConnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		//LOG.info("connected to server "+address);
		if(stopped) {
			e.getChannel().close();
			return;
		}
		timeoutMillis=firstReconnectionTimeout;
		initDecoder();
		channel = e.getChannel();
		if(channelGroup!=null)
			channelGroup.add(channel);
		super.channelConnected(ctx, e);
		timesConnected.incrementAndGet();
		connectionState = State.CONNECTED;
		lastSuccessTimestamp = System.currentTimeMillis();
		
		ArrayList<OP> q = new ArrayList<OP>(opq.size()) ;
		opq.drainTo(q);

		for (OP op : q) {
			if(!execute(op)) {
				failureRate.add(100.);
				op.notifyListener(new IOException("Unable to schedule on connected"));
			}
		}

		
		onConnected();
	}
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		timesDisonnected.incrementAndGet();
		//LOG.info("Disconnected from server "+address);
		channel = null;
		initDecoder();
		//listener should pick the ops list
		onDisconnected();
		super.channelDisconnected(ctx, e);
		dropInQueue(new IOException("Disconnected"));
		connect();
	}

	private void dropInQueue(Throwable failureReason) {
		//we drop requests if nobody pick em up
		ArrayList<OP> q = new ArrayList<OP>(opq.size()) ;
		opq.drainTo(q);
		opsDropped.addAndGet(q.size());
		for (OP op : q) {
			failureRate.add(100.);
			op.notifyListener(failureReason);
		}
	}
	
	@ManagedAttribute
	public boolean isAvailable() {
		if(LOG.isDebugEnabled())
			LOG.debug("isAvailable(), stopped={}, channel={}, opq.size={}", new Object[] {stopped, channel, opq.size()});
		if(stopped)
			return false;
		if(channel == null)
			return false;
		if(opq.size()>maxOps)
			return false;
		return true;
	}
	/**
	 * Execute operation on the connection
	 * @param op
	 * @return false if unable to queue
	 */
	public boolean execute(OP op) {
		if(op.isFinished()) {
			//Called with expired or finished op. the caller already was notified, bail
			return true;
		}
		if(!isAvailable())
			return false;
		op.setStatsCallback(this);
		opsQueued.incrementAndGet();
		if(isConnected()) {
			try {
				Object buf = op.resetAndBuildRequest();
				if(LOG.isDebugEnabled()) {
					LOG.debug(address.toString()+"<--"+buf.toString());
				}
				opq.add(op);
				channel.write(buf); //could be a race condition, should not be a big problem
			} catch (Exception e) {
				opq.remove(op);
				failureRate.add(100.);
				op.notifyListener(e);
				return true; //prevent from scheduling on another connection in a slice 
			}
		} else {
			if(LOG.isDebugEnabled()) {
				LOG.debug("Not connected, queueing op {}", op);
			}
			opq.add(op);
		}
		return true;
	}

	abstract protected void onMessage(Object message, MessageEvent e)throws ProtocolException;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if(e.getChannel()!=channel) {
			LOG.error("Message for another channel? "+e.getMessage().getClass().getName());
			return; //cut this short
		}
		Object m = e.getMessage();
		onMessage(m, e);

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

		opsFailed.incrementAndGet();
		lastFailureReason = cause.getMessage();
		LOG.error("Exception:", cause);
		e.getChannel().close();
	}

	@Override
	public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e)
			throws Exception {
		bytesSent.addAndGet(e.getWrittenAmount());
	}
	
	/*
	 * Callbacks for changed connection state notification.
	 * We use it in {@link Slice.java}
	 */
	/**
	 * connection closed.
	 * the implementation should drain
	 * queue otherwise it will be cleared and ops' callbacks notified
	 */
	public void onDisconnected() {
		//You need to implement this method if you need to be notified about connection events
	}
	public void onConnected() {
		//You need to implement this method if you need to be notified about connection events
	}
	/**
	 * connection failure (Unable to establish connection).
	 * If the connection is configured to accept ops while connecting the implementation should drain
	 * queue otherwise it will be cleared and ops' callbacks notified
	 */
	public void onConnectionFailure() {
		//You need to implement this method if you need to be notified about connection events
	}
	public void start() {
		connect();
	}
	public void stop() {
		connectionState = State.CLOSING;
		stopped = true;
		if(channel!=null) {
			channel.close();
			channel=null;
		}
		MBeanExporter.unregister(objectName);
	}
	/**
	 * Disconnect the connection. It will immediately go back up if persistent
	 */
	@ManagedOperation
	public void disconnect() {
		if(channel!=null) {
			channel.close();
			channel=null;
		}
	}
	@ManagedAttribute
	public boolean isConnected() {
		return channel!=null;
	}
	@ManagedAttribute
	public int getQueueLength() {
		return opq.size();
	}
	@ManagedAttribute
	public String getAddress() {
		return address.toString();
	}

	public ChannelGroup getChannelGroup() {
		return channelGroup;
	}

	public void setChannelGroup(ChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	public void setAddress(String host, int port) {
		this.host=host;
		this.port=port;
	}
	public void setAddress(InetSocketAddress address, int port) {
		this.host=null;
		this.address=address;
		this.port=port;
	}

	@ManagedAttribute(description="error rate in percent, 0 = perfect connection")
	public double getFailureRate() {
		return failureRate.get();
	}

	
}
