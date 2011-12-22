package com.adbrite.jmx;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.jmx.annotations.MBean;

public class MBeanExporter {
	static final Logger LOG = LoggerFactory.getLogger(MBeanExporter.class);
	public static ObjectName export(Object object) {
		String mbeanName=null;
		if(object.getClass().isAnnotationPresent(MBean.class)) {
			MBean mbean = object.getClass().getAnnotation(MBean.class);
			mbeanName=mbean.objectName();
		}
		if(mbeanName==null || mbeanName.isEmpty()) {
			String pkg = object.getClass().getPackage().getName();
			String type = object.getClass().getSimpleName();
			mbeanName =  pkg+":type=" + type;
		}
		return export(object, mbeanName);
	}
	public static ObjectName export(Object object, String mbeanName) {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName nameObj = new ObjectName(mbeanName );
			//if(object.getClass().isAnnotationPresent(MBean.class)) {
			//	object = new ABDynaMBean(object);
			//}
			mbs.registerMBean(new ABDynaMBean(object), nameObj );
			return nameObj;
			} catch (Exception e) {
				LOG.error("Object with name {} already exists, not registering the new one", mbeanName);
				return null;
			}
	}
	public static void unregister(ObjectName name) {
		if(name==null)
			return;
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.unregisterMBean(name);
		} catch (MBeanRegistrationException e) {
			LOG.info("Unable to unregister mbean", e.getMessage());
		} catch (InstanceNotFoundException e) {
			LOG.info("Unable to unregister mbean", e.getMessage());
		}
	}
}
