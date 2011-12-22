package com.adbrite.netty.memcache;

public class NormalizedKey {
	protected static final int KEY_MAX_SIZE = 250;
	
	private final byte[] bytes;

	public NormalizedKey(String key) {
		int length = key.length();
		if (length > KEY_MAX_SIZE) {
			length = KEY_MAX_SIZE;
		}
		bytes = new byte[length];
		for (int i = 0; i < length; ++i) {
			char ch = key.charAt(i);
			if (Character.isISOControl(ch) || ch == ' ') {
				bytes[i] = '_';
			} else {
				bytes[i] = (byte) ch;
			}
		}
	}

	/**
	 * DO NOT MODIFY! 
	 * @return
	 */
	public byte[] getBytes() {
		return bytes;
	}
	
	public int length() {
		return bytes.length;
	}

	@Override
	public String toString() {
		return new String(bytes);
	}

}
