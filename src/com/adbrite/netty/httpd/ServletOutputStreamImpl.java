package com.adbrite.netty.httpd;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import org.jboss.netty.buffer.ChannelBufferOutputStream;

public class ServletOutputStreamImpl extends ServletOutputStream {
	
	private final ChannelBufferOutputStream _out;
	
	public ServletOutputStreamImpl(ChannelBufferOutputStream out) {
		_out = out;
	}

	@Override
	public void write(int b) throws IOException {
		_out.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		_out.write(b);
	}
	@Override
	public void write(byte[] arg0, int arg1, int arg2) throws IOException {
		_out.write(arg0, arg1, arg2);
	}
	@Override
	public void print(String s) throws IOException {
		_out.write(s.getBytes());
	}
	@Override
	public void flush() throws IOException {
		_out.flush();
	}

}
