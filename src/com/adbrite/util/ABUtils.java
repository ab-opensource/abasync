package com.adbrite.util;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class ABUtils {
    public static final double NANOSECONDS_IN_A_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1);
	public final static byte[] BLANK_GIF_BYTES = {
	    71,   73,   70,   56,   57,   97,    1,    0,
	     1,    0, -128,    0,    0,    0,    0,    0,
	    -1,   -1,   -1,   33,   -7,    4,    1,    0,
	     0,    0,    0,   44,    0,    0,    0,    0,
	     1,    0,    1,    0,   64,    2,    1,   68,
	     0,   59};
	public static final Charset UTF8 = Charset.forName("UTF-8");

}
