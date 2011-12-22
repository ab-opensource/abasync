package com.adbrite.netty.httpd;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.adbrite.util.ABUtils;

public class QueryString {

	private Map<String, List<String>> parameters = new TreeMap<String, List<String>>();
	private String path;
	private String queryString=null;

	public QueryString(String uri) {
		// Parse query string
        int pathEndPos = uri.indexOf('?');
        if (pathEndPos < 0) {
            path = uri;
            return;
        }
        else {
            path = uri.substring(0, pathEndPos);
            queryString = uri.substring(pathEndPos+1);
        }
		String pairs[] = ABUtils.stringSplit(queryString, '&');
		for (String pair : pairs) {
			String name;
			String value;
			int pos = pair.indexOf('=');
			// for "n=", the value is "", for "n", the value is null
			try {
				if (pos == -1) {
					name = URLDecoder.decode(pair, "UTF-8");
					value = null;
				} else {
					name = URLDecoder.decode(pair.substring(0, pos), "UTF-8");
					value = URLDecoder.decode(
							pair.substring(pos + 1, pair.length()), "UTF-8");
				}
				List<String> list = parameters.get(name);
				if (list == null) {
					list = new ArrayList<String>();
					parameters.put(name, list);
				}
				list.add(value);

			} catch (UnsupportedEncodingException e) {
				// Not really possible, throw unchecked
				throw new IllegalStateException("No UTF-8");
			} catch (IllegalArgumentException e) {
				// do nothing,skip the parameter
			}
		}
	}

	public QueryString() {
	}

	public String getParameter(String name) {
		List<String> values = parameters.get(name);
		if (values == null)
			return null;

		if (values.size() == 0)
			return "";

		return values.get(0);
	}

	public String[] getParameterValues(String name) {
		List<String> values = parameters.get(name);
		if (values == null)
			return null;

		return (String[]) values.toArray(new String[values.size()]);
	}

	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(parameters.keySet());
	}

	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> map = new TreeMap<String, String[]>();
		for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
			List<String> list = entry.getValue();
			String[] values;
			if (list == null)
				values = null;
			else
				values = (String[]) list.toArray(new String[list.size()]);
			map.put(entry.getKey(), values);
		}
		return map;
	}

	public void put(String key, String value) {
		List<String> list = parameters.get(key);
		if (list == null) {
			list = new ArrayList<String>();
			parameters.put(key, list);
		}
		list.add(value);
	}

	public void put(String key, Collection<String> values) {
		List<String> list = parameters.get(key);
		if (list == null) {
			list = new ArrayList<String>();
			parameters.put(key, list);
		}
		list.addAll(values);
	}

	public String getPath() {
		return path;
	}

	public String getQueryString() {
		return queryString;
	}
}
