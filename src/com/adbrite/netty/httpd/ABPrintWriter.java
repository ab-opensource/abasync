package com.adbrite.netty.httpd;

import java.io.PrintWriter;
import java.util.Locale;

import org.jboss.netty.buffer.ChannelBufferOutputStream;

public class ABPrintWriter extends PrintWriter {
	private final ChannelBufferOutputStream os;

	public ABPrintWriter(ChannelBufferOutputStream channelBufferOutputStream) {
		super(channelBufferOutputStream, false);
		os = channelBufferOutputStream;
	}

	@Override
	public void write(int c) {
		super.write(c);
		flush();
	}

	@Override
	public void write(char[] buf, int off, int len) {
		super.write(buf, off, len);
		flush();
	}

	@Override
	public void write(char[] buf) {
		super.write(buf);
		flush();
	}

	@Override
	public void write(String s, int off, int len) {
		super.write(s, off, len);
		flush();
	}

	@Override
	public void write(String s) {
		super.write(s);
		flush();
	}

	@Override
	public void print(boolean b) {
		super.print(b);
		flush();
	}

	@Override
	public void print(char c) {
		super.print(c);
		flush();
	}

	@Override
	public void print(int i) {
		super.print(i);
		flush();
	}

	@Override
	public void print(long l) {
		super.print(l);
		flush();
	}

	@Override
	public void print(float f) {
		super.print(f);
		flush();
	}

	@Override
	public void print(double d) {
		super.print(d);
		flush();
	}

	@Override
	public void print(char[] s) {
		super.print(s);
		flush();
	}

	@Override
	public void print(String s) {
		super.print(s);
		flush();
	}

	@Override
	public void print(Object obj) {
		super.print(obj);
		flush();
	}

	@Override
	public void println() {
		super.println();
		flush();
	}

	@Override
	public void println(boolean x) {
		super.println(x);
		flush();
	}

	@Override
	public void println(char x) {
		super.println(x);
		flush();
	}

	@Override
	public void println(int x) {
		super.println(x);
		flush();
	}

	@Override
	public void println(long x) {
		super.println(x);
		flush();
	}

	@Override
	public void println(float x) {
		super.println(x);
		flush();
	}

	@Override
	public void println(double x) {
		super.println(x);
		flush();
	}

	@Override
	public void println(char[] x) {
		super.println(x);
		flush();
	}

	@Override
	public void println(String x) {
		super.println(x);
		flush();
	}

	@Override
	public void println(Object x) {
		super.println(x);
		flush();
	}

	@Override
	public PrintWriter printf(String format, Object... args) {
		PrintWriter r = super.printf(format, args);
		flush();
		return r;
	}

	@Override
	public PrintWriter printf(Locale l, String format, Object... args) {
		PrintWriter r = super.printf(l, format, args);
		flush();
		return r;
	}

	@Override
	public PrintWriter format(String format, Object... args) {
		PrintWriter r = super.format(format, args);
		flush();
		return r;
	}

	@Override
	public PrintWriter format(Locale l, String format, Object... args) {
		PrintWriter r = super.format(l, format, args);
		flush();
		return r;
	}

	@Override
	public PrintWriter append(CharSequence csq) {
		PrintWriter r = super.append(csq);
		flush();
		return r;
	}

	@Override
	public PrintWriter append(CharSequence csq, int start, int end) {
		PrintWriter r = super.append(csq, start, end);
		flush();
		return r;
	}

	@Override
	public PrintWriter append(char c) {
		PrintWriter r = super.append(c);
		flush();
		return r;
	}

}
