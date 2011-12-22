package com.adbrite.netty.httpd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Resource;
import javax.servlet.GenericServlet;

import org.jboss.netty.channel.ChannelUpstreamHandler;

import com.adbrite.collections.TrieMap;
import com.adbrite.counters.CounterEMA;
import com.adbrite.jmx.MBeanExporter;
import com.adbrite.jmx.annotations.ManagedAttribute;

/**
 * Dispatch table for http. handlers could be one of the following types:
 * {@link com.adbrite.servlet.Servlet.Controller}
 * {@link javax.servlet.GenericServlet}
 * @author apesternikov
 *
 */
public class HttpDispatchTable {

	@Resource(name="workerThreadPool")
	Executor workerExecutor;

	
	private ArrayList<Item> allItems = new ArrayList<HttpDispatchTable.Item>();
	
	public static class Item {
		final String path;
		@ManagedAttribute
		public String getPath() {
			return path;
		}
		final ChannelUpstreamHandler handler;
		final AtomicLong count = new AtomicLong();
		@ManagedAttribute
		public long getCount() {
			return count.get();
		}
		
		final AtomicLong inProgress = new AtomicLong();
		@ManagedAttribute
		public long getInProgress() {
			return inProgress.get();
		}
		final CounterEMA avgLatencyMs = new CounterEMA();
		@ManagedAttribute
		public double getAvgLatencyMs() {
			return avgLatencyMs.get();
		}
		public Item(String path, ChannelUpstreamHandler handler) {
			this.path = path;
			this.handler = handler;
//			String classname = handler.getClass().getSimpleName();
//			if(classname==null) {
//				classname = handler.getClass().getSuperclass().getSimpleName();
//			}
			String mbeanname = "com.adbrite.http.handlers:path="+path;
			try {
			MBeanExporter.export(this, mbeanname);
			}catch (RuntimeException e) {
			}
		}
	}
	
	private final TrieMap<Item> _table = new TrieMap<Item>();
	private Item defaultHandler;

	public void setDefaultHandler(Object handler) {
		this.defaultHandler = new Item("404", buildHandler(handler));
		allItems.add(this.defaultHandler);
	}
	
	private void setHandler(String path, ChannelUpstreamHandler handler) {
		Item item = new Item(path, handler);
		_table.put(path, item);
		allItems.add(item);
	}

	private ChannelUpstreamHandler buildHandler(Object obj) {
		if(obj instanceof ChannelUpstreamHandler) {
			return (ChannelUpstreamHandler) obj;
		}
		else if(obj instanceof Controller) {
			return new ControllerHandler((Controller) obj, workerExecutor);
		}
		else if(obj instanceof AsyncController) {
			return new AsyncControllerHandler((AsyncController) obj);
		}
		else if(obj instanceof GenericServlet) {
			return new ServletHandler((GenericServlet) obj, workerExecutor);
		} else {
			throw new RuntimeException("Unsupported handler type "+obj.getClass().getName());
		}
	}
	
	private void setHandler(Map<String, Object> servlet) {
		Set<String> keys = servlet.keySet();
		for (String key : keys) {
			Object obj = servlet.get(key);
			ChannelUpstreamHandler handler = buildHandler(obj);
			setHandler(key, handler);
		}
	}
	
	public void setHandlers(Map<String, Object> servlets[]) {
		for (Map<String, Object> map : servlets) {
			setHandler(map);
		}
	}

	public Item get(String key) {
		Item handler;
		handler = _table.floor(key);
		if(handler==null)
			handler=defaultHandler;
		return handler;
	}

	public Collection<Item> items() {
		return allItems;
	}

}
