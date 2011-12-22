package com.adbrite.netty.httpclient;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.adbrite.netty.AsyncCallback;

public class PersistentClient implements HttpClient {

	private static final Throwable BUSY = new IOException("No connections available");
//	private String host;
//	private int port;
//	Executor bossExecutor;
//	Executor workerExecutor;
	//Connections ready to serve request
	private ArrayList<NettyHttpConnection> conns = new ArrayList<NettyHttpConnection>();
	private LinkedBlockingQueue<NettyHttpConnection> connsReady = new LinkedBlockingQueue<NettyHttpConnection>();
	
	public PersistentClient(String host, int port,
			Executor bossExecutor, Executor workerExecutor, int maxConns) throws UnknownHostException {
//		this.host = host;
//		this.port = port;
//		this.bossExecutor = bossExecutor;
//		this.workerExecutor = workerExecutor;
		for(int i=0; i<maxConns; i++) {
			NettyHttpConnection c = new NettyHttpConnection(host, port, bossExecutor, workerExecutor, i) {
				public void onConnected() {
					connsReady.add(this);
				};
				@Override
				public void onDisconnected() {
					connsReady.remove(this); //FIXME: O(N) Perhaps not a big deal for 3 connections
				}
			};
			c.start();
			conns.add(c);
		}
	}
	
	public void stop() {
		for (NettyHttpConnection c : conns) {
			c.stop();
		}
	}
	public void execute(HttpRequest request, int timeoutMs,
				final AsyncCallback<HttpResponse> callback) {
		final NettyHttpConnection c = connsReady.poll();
		if(c==null) {
			callback.notifyListener(BUSY);
			return;
		}
		c.execute(new HttpOperation(request, timeoutMs) {
			@Override
			public void operationCompleted() {
				if(isSucess()) {
					callback.notifyListener(getResponse());
				} else {
					callback.notifyListener(getFailureReason());
				}
				if(c.isAvailable()) {
					connsReady.add(c);
				}
			}
		});

	}
	@Override
	public boolean isAvailable() {
		return !connsReady.isEmpty();
	}

	@Override
	public void setAddress(String host, int port) {
		for (NettyHttpConnection conn : conns) {
			conn.setAddress(host, port);
		}
	}

	@Override
	public double getFailureRate() {
		double rate=100.;
		for(NettyHttpConnection conn: conns) {
			double failureRate = conn.getFailureRate();
			if(rate>failureRate)
				rate = failureRate;
		}
		return rate;
	}

}
