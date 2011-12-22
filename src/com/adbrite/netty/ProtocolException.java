package com.adbrite.netty;

import java.io.IOException;

public class ProtocolException extends IOException {

	private static final long serialVersionUID = -2521612289664579702L;
	
	public ProtocolException(String message) {
		super(message);
	}
	
	public ProtocolException(String message, Throwable cause) {
		super(message, cause);
	}
	
	

}
