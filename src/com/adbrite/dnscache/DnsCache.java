package com.adbrite.dnscache;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsCache {

	public static InetAddress getByName(String host) throws UnknownHostException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public static InetAddress[] getAllByName(String host) throws UnknownHostException{
		// TODO Auto-generated method stub
		return null;
	}

	public static String getDatacenterId() {
		return "dc";
	}
	
	public static String getDomain() {
		return ".domain";
	}

	public static String getDatacenterHostName(String pShortHostName) {
		if (pShortHostName.indexOf('.') > 0) {
			return pShortHostName;
		}
		final String shortHostName = pShortHostName.toLowerCase();
		if (shortHostName.equals("ads") || shortHostName.equals("click")) {
			return pShortHostName + '-' + getDatacenterId() + getDomain();
		} else {
			return pShortHostName + '.' + getDatacenterId() + getDomain();
		}
	}

}
