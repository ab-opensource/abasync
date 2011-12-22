package com.adbrite.netty.httpd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.util.ABUtils;

public class HttpLogger {
	private static final Logger LOG = LoggerFactory.getLogger(HttpLogger.class);
	private SimpleDateFormat logformat = new SimpleDateFormat(
			"dd/MMM/yyyy:HH:mm:ss Z");

	private SimpleDateFormat logfileformat = new SimpleDateFormat(
			"yyyy-MM-dd-HH-mm");

	private LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<String>();
	//private volatile boolean exit = false;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	private Runnable logWriter = new Runnable() {
		private long lastHour = 0;
		private FileWriter os = null;
		BufferedWriter bw;

		@Override
		public void run() {
			if (!queue.isEmpty()) {
				ArrayList<String> q = new ArrayList<String>(queue.size());
				queue.drainTo(q);
				long time = System.currentTimeMillis();
				long hour = time / (1000L * 60 * 60);
				if (hour != lastHour) {
					lastHour = hour;
					String filename = logPrefix
							+ logfileformat.format(new Date(hour
									* (1000L * 60 * 60))) + ".log";
					try {
						if (os != null) {
							try {
								bw.close();
							} catch (IOException e) {
								LOG.error("Unable to close log file ", e);
							}
							os = null;
							bw = null;
						}
						try {
							os = new FileWriter(filename, true);
						}catch (FileNotFoundException e) {
							//try to create directory
							int slashidx = filename.lastIndexOf('/');
							if(slashidx==-1)
								throw e;
							String dirname = filename.substring(0, slashidx);
							boolean success = (new File(dirname)).mkdirs();
							if(!success) {
								throw new IOException("Unable to create directory "+dirname);
							}
							os = new FileWriter(filename, true);
						}
						bw = new BufferedWriter(os);
					} catch (IOException e) {
						LOG.error("Unable to open file " + filename, e);
					}
				}
				if (os != null) {
					try {
						for (String string : q) {
							bw.write(string);
							bw.write('\n');
						}
						bw.flush();
					} catch (IOException e) {
						LOG.error("Unable to write log file", e);
					}
				}
			}
		}
	};
	private String logPrefix;

	public HttpLogger(String logPrefix) {
		this.logPrefix = logPrefix;
	}

	public void log(HttpRequest request, HttpResponse response,
			SocketAddress ra, long startTimeNs) {
		String ip;
		if (ra instanceof InetSocketAddress) {
			InetSocketAddress isa = (InetSocketAddress) ra;
			ip = isa.getAddress().getHostAddress();
		} else {
			ip = ra.toString();
		}
		StringBuilder sb = new StringBuilder();
		sb.append(ip)
		.append(" - - [")
		.append(logformat.format(new Date()))
		.append("] \"")
		.append(request.getMethod().toString())
		.append(' ')
		.append(request.getUri())
		.append(' ')
		.append(request.getProtocolVersion().getText())
		.append('"')
		.append(' ').append(response.getStatus().getCode())
		.append(' ').append(HttpHeaders.getContentLength(response));
		String refheader = request.getHeader(Names.REFERER);
		sb.append(" \"").append(refheader != null ? refheader : "-")
				.append('"');
		String ua = request.getHeader(Names.USER_AGENT);
		sb.append(" \"").append(ua != null ? ua : "-").append('"');
		long endTime = System.nanoTime();
		sb.append(' ').append(TimeUnit.NANOSECONDS.toMillis(endTime - startTimeNs)).append("ms");
		queue.offer(sb.toString());
		/*
		if(LOG.isDebugEnabled()) {
			LOG.debug("Request:" + request);
			LOG.debug("Response:" + response);
		}
		*/
	}

	@PostConstruct
	public void start() {
		executor.scheduleWithFixedDelay(logWriter, 500, 500, TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	public void stop() {
		executor.shutdown();
	}

}
