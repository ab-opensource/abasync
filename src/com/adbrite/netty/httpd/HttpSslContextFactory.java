package com.adbrite.netty.httpd;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpSslContextFactory {
	private static final Logger LOG = LoggerFactory.getLogger(HttpSslContextFactory.class);
	private final SSLContext _serverContext;
	
	public HttpSslContextFactory(String keyFilePath) {
		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		if (null == algorithm) {
			algorithm = "SunX509";
		}
		
		SSLContext serverContext = null;
		FileInputStream fin = null;
		char[] password = {'@', 'd', 'b', 'r', '1', 't', '3'};
		try {
			KeyStore ks = KeyStore.getInstance("PKCS12");
			fin = new FileInputStream(keyFilePath);
			ks.load(fin, password);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
			kmf.init(ks, password);
			serverContext = SSLContext.getInstance("SSL");
			serverContext.init(kmf.getKeyManagers(), null, null);
		} catch (Exception e) {
			LOG.error("Unable to initialize SSL Context", e);
			throw new Error(e);
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		_serverContext = serverContext;
	}
	
	public SSLContext getServerContext() {
		return _serverContext;
	}
}
