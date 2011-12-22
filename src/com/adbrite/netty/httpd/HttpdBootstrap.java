package com.adbrite.netty.httpd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.adbrite.dnscache.DnsCache;

public class HttpdBootstrap {
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpdBootstrap.class);

	/**
	 * load properties, configs and start http server
	 * @param configxmls
	 * @throws Exception
	 */
	public static ApplicationContext bootstrap(String[] configxmls) throws Exception {
		PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
		Resource defaultResource = new ClassPathResource("applicationContext.xml.default.properties");
		Resource dcResource = new ClassPathResource("applicationContext.xml." + DnsCache.getDatacenterId() + ".properties");
		if(!defaultResource.exists()) {
			LOG.error("Unable to open "+defaultResource.getFilename()+" in CLASSPATH");
			throw new RuntimeException();
		}
		if(dcResource.exists()) {
			cfg.setLocations(new Resource[] {defaultResource, dcResource});
		} else {
			cfg.setLocations(new Resource[] {defaultResource});
		}

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.addBeanFactoryPostProcessor(cfg);
		context.setConfigLocations(configxmls);
		context.refresh();

		HttpServer server = (HttpServer)context.getBean("httpServer",HttpServer.class);
		return context;
	}
}
