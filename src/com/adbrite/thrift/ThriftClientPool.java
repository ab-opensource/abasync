package com.adbrite.thrift;

import java.io.IOException;

import org.apache.thrift.async.TAsyncClientFactory;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;

import com.adbrite.util.ObjectPool;

public class ThriftClientPool<Client extends org.apache.thrift.async.TAsyncClient> extends ObjectPool<Client>{

	private final org.apache.thrift.async.TAsyncClientFactory<Client> factory;
	private final String host;
	private final int port;
	private final int timeout;
	
	public String getHost() {
		return host;
	}
	public int getPort() {
		return port;
	}
	
	public ThriftClientPool(TAsyncClientFactory<Client> factory, String host, int port, int timeout) {
		this.factory = factory;
		this.host=host;
		this.port=port;
		this.timeout=timeout;
	}

	@Override
	protected Client createObject() {
		try {
			//There is a comment in TNonblockingSocket:
			//this implementation never uses blocking operations so it is unused.
			TNonblockingTransport transport = new TNonblockingSocket(host, port, timeout);
			Client client = factory.getAsyncClient(transport);
			client.setTimeout(timeout);
			return client;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected boolean validateObject(Client obj) {
		return !obj.hasError();
	};
	
	@Override
	protected void discardObject(Client obj) {
		obj.close();
	};

}
