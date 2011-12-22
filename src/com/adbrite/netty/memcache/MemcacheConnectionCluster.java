package com.adbrite.netty.memcache;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.zip.CRC32;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import com.adbrite.dnscache.DnsCache;
import com.google.common.annotations.VisibleForTesting;

public class MemcacheConnectionCluster {
	private static final Logger LOG = LoggerFactory.getLogger(MemcacheConnectionCluster.class);
	private Slice[] slices;
	
	private String baseHostname;
	private int port;

	public String partitionHostname(int pPartitionIndex) {
		return DnsCache.getDatacenterHostName(String.format(baseHostname, pPartitionIndex + 1));
	}

	//TODO: 
	//Create async dns update notification mechanism, update slices if dns updated
	@PostConstruct
	public void start() throws UnknownHostException {
		ArrayList<Slice> v = new ArrayList<Slice>(10);
		int i=0;
		try {
			for (i=0; i<10; i++) {
				String hostname = partitionHostname(i);
				InetAddress[] ips = DnsCache.getAllByName(hostname);
				InetSocketAddress[] addrs = new InetSocketAddress[ips.length];
				for(int j=0; j<ips.length; j++) {
					addrs[j] = new InetSocketAddress(ips[j], port);
				}
				Slice slice = new Slice(addrs, bossExecutor, workerExecutor, i);
				v.add(slice);
			}
		} catch (UnknownHostException ex) {
			if(i<1 || i>=10) {
				throw new IllegalStateException("Number of memcache partitions discovered: "+i);
			}
		}
		slices = v.toArray(new Slice[0]);
		for (Slice slice : slices) {
			slice.start();
		}
	}
	
	@PreDestroy
	public void stop() {
		for (Slice slice : slices) {
			slice.stop();
		}
	}
	
	/**
	 * Run op on the proper slice
	 * @param key
	 * @return
	 */
	public void execute(MemcacheSingleKeyOperation op) {
		assert (slices.length > 0);

		int index = 0;
		final CRC32 crc = new CRC32();
		byte[] key = op.getKey().getBytes();
		crc.update(key);
		final long crcValue = crc.getValue();
		long hash = (crcValue >> 16) & 0x7fff;
		if (hash == 0) {
			hash = 1;
		}
		index = (int) (hash % slices.length);

		Slice slice = slices[index];
		if(LOG.isDebugEnabled()) {
			LOG.debug("executing "+op.toString()+" on slice "+index);
		}
		slice.execute(op);
	}
	
	@VisibleForTesting
	@Resource(name="bossThreadPool")
	Executor bossExecutor;

	@VisibleForTesting
	@Resource(name="workerThreadPool")
	Executor workerExecutor;
	
	@Required
	public void setBaseHostname(String baseHostname) {
		this.baseHostname = baseHostname;
	}

	public String getBaseHostname() {
		return baseHostname;
	}

	@Required
	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}
	
	public void setBossExecutor(Executor bossExecutor) {
		this.bossExecutor = bossExecutor;
	}
	public void setWorkerExecutor(Executor workerExecutor) {
		this.workerExecutor = workerExecutor;
	}
	public Slice[] getSlices() {
		return slices;
	}
	
	public void setTimeoutMs(int timeout) {
		MemcacheOperation.setTimeoutMs(timeout);
	}
	public int getTimeoutMs() {
		return MemcacheOperation.getTimeoutMs();
	}
}
