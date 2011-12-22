package com.adbrite.netty.memcache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.jmx.MBeanExporter;

/**
 * This is a slice of memcached servers serving the same keys region. the Slice
 * monitors underlying memcached connections state, queue lengths and route
 * requests to least loaded server.
 * Slice can perform MemcacheReadOnly operations only
 * 
 * @author apesternikov
 * 
 */
public class Slice {
	private static final IOException NO_CONNECTION_AVAILABLE = new IOException("No connection available in a slice");

	private static final Logger LOG = LoggerFactory.getLogger(Slice.class);

	private NettyMemcacheConnection[] connections;
	
	private AtomicInteger Nconnected = new AtomicInteger(0);

	public Slice(InetSocketAddress[] addrs, Executor bossExecutor,
			Executor workerExecutor, final int sliceNumber) {
		connections = new NettyMemcacheConnection[addrs.length];
		for (int i = 0; i < addrs.length; i++) {
			String mbeanName =  "com.adbrite.netty.memcache:type=" + NettyMemcacheConnection.class.getSimpleName() + ",slice="+sliceNumber+",address="+addrs[i].getAddress().toString() ;
			connections[i] = new NettyMemcacheConnection(addrs[i],
					bossExecutor, workerExecutor, mbeanName) {
				public void onConnected() {
					Nconnected.incrementAndGet();
				};
				public void onDisconnected() {
					int N = Nconnected.decrementAndGet();
					if(N==0) {
						LOG.error("No live memcache servers in slice "+sliceNumber);
					}
					ArrayList<MemcacheOperation> q = new ArrayList<MemcacheOperation>(opq.size()) ;
					opq.drainTo(q);
					for (MemcacheOperation op : q) {
						if(!op.isFinished()) {
							Slice.this.execute(op);
						}
					}
				};
			};
		}
	}

	public void start() {
		for (NettyMemcacheConnection conn : connections) {
			conn.start();
		}
	}

	public void stop() {
		for (NettyMemcacheConnection conn : connections) {
			conn.stop();
		}
	}

	private AtomicLong idx = new AtomicLong();

	/**
	 * Execute on the next available server or call failure callback immediately
	 * implements round-robin next server selection
	 * This implementation is simple and fast if most servers are up,
	 * but horribly inefficient if many of the servers are down.
	 * 
	 * @param op
	 */
	public void execute(MemcacheOperation op) {
		for (int j = 0; j < connections.length; j++) {
			long i = idx.incrementAndGet();
			NettyMemcacheConnection c = connections[(int) (i % connections.length)];
			if (c.execute(op))
				return;
		}
		op.notifyListener(NO_CONNECTION_AVAILABLE);
	}

	public NettyMemcacheConnection[] getConnections() {
		return connections;
	}

	public int getAvailableCount() {
		int cnt = 0;
		for (int j = 0; j < connections.length; j++) {
			if(connections[j].isAvailable())
				cnt++;
		}
		return cnt;
	}
}
