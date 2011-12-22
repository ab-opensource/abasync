package com.adbrite.netty.httpd;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

import com.adbrite.netty.AsyncCallback;

/**
 * Very simple servlet for serving static files. 
 * Does not handle If-Modified-Since so should be used as a CDN source ONLY!
 * TODO: make it cache-friendly
 * @author apesternikov
 *
 */
public class StaticFileController implements AsyncController {

	private static final long serialVersionUID = -65058030006731865L;
	private static final Logger _log = LoggerFactory
			.getLogger(StaticFileController.class);
	private byte[] content;
	private String contentType;
	private Resource resource;

	public Resource getResource() {
		return resource;
	}

	@Required
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	@PostConstruct
	public void postConstruct() throws IOException {
		InputStream in = resource.getInputStream();
		try {
			content = IOUtils.toByteArray(in);
		} finally {
			in.close();
		}
		if (resource.getFilename().endsWith(".html")) {
			contentType = "text/html; charset=UTF-8";
		} else if (resource.getFilename().endsWith(".js")) {
			contentType = "text/javascript; charset=UTF-8";
		} else if (resource.getFilename().endsWith(".swf")) {
			contentType = "application/x-shockwave-flash";
		} else if (resource.getFilename().endsWith(".xsl")) {
			contentType = "text/xml";
		} else if (resource.getFilename().endsWith(".png")) {
			contentType = "image/png";
		} else {
			contentType = "text/plain; charset=UTF-8";
		}
	}

	@Override
	public void processAsyncRequest(ABHttpRequest request,
			AsyncCallback<HttpResponse> responseHandler) throws Exception {
		responseHandler.notifyListener(
				new ABHttpResponse(contentType, content)
				);
	}

}
