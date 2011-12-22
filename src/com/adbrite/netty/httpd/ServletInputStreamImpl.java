package com.adbrite.netty.httpd;

import java.io.IOException;

import javax.servlet.ServletInputStream;

import org.jboss.netty.buffer.ChannelBufferInputStream;

public class ServletInputStreamImpl extends ServletInputStream {
	
	private final ChannelBufferInputStream _in;
	
	public ServletInputStreamImpl(ChannelBufferInputStream in)  {
		_in = in;
	}

	@Override
	public int read() throws IOException {
		return _in.read();
	}
}
