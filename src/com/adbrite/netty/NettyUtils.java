package com.adbrite.netty;

import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBuffers;

public class NettyUtils {
	private static final Charset CHARSET = Charset.forName("ISO-8859-1");

	/**
	 * Read next line separated by CRLF
	 * 
	 * @param data
	 * @return the line in ChannelBuffer with CRLF stripped
	 * CRLF is consumed from the buffer.
	 * @throws ProtocolException
	 */
	public static String readStringFromBuffer(ChannelBuffer data) throws ProtocolException {
			int nlidx = data.bytesBefore((byte) '\r');
			if(nlidx<0)
				return null;
			if(data.readableBytes()<nlidx+2)
				return null;
			ChannelBuffer string = data.readSlice(nlidx);
			byte[] crlf = new byte[2];
			data.readBytes(crlf);
			if(crlf[0]!='\r' && crlf[1]!='\n')
				throw new ProtocolException("expecting CRLF");
			return string.toString(CHARSET);
	}
	
	public static boolean startsWith(ChannelBuffer bigger, ChannelBuffer smaller) {
        final int aLen = bigger.readableBytes();
        final int bLen = smaller.readableBytes();
        if(aLen<bLen)
        	return false;
        final int uintCount = bLen >>> 2;
        final int byteCount = bLen & 3;

        int aIndex = bigger.readerIndex();
        int bIndex = smaller.readerIndex();

        if (bigger.order() == smaller.order()) {
            for (int i = uintCount; i > 0; i --) {
                long va = bigger.getUnsignedInt(aIndex);
                long vb = smaller.getUnsignedInt(bIndex);
                if (va != vb) {
                    return false;
                }
                aIndex += 4;
                bIndex += 4;
            }
        } else {
            for (int i = uintCount; i > 0; i --) {
                long va = bigger.getUnsignedInt(aIndex);
                long vb = ChannelBuffers.swapInt(smaller.getInt(bIndex)) & 0xFFFFFFFFL;
                if (va != vb) {
                    return false;
                }
                aIndex += 4;
                bIndex += 4;
            }
        }

        for (int i = byteCount; i > 0; i --) {
            byte va = bigger.getByte(aIndex);
            byte vb = smaller.getByte(bIndex);
            if (va != vb) {
                return false;
            }
            aIndex ++;
            bIndex ++;
        }

        return true;
	}

	public static boolean startsWith(ChannelBuffer bigger, byte[] smaller) {
        final int aLen = bigger.readableBytes();
        final int bLen = smaller.length;
        if(aLen<bLen)
        	return false;
        final int byteCount = bLen;

        int aIndex = bigger.readerIndex();
        int bIndex = 0;

        for (int i = byteCount; i > 0; i --) {
            byte va = bigger.getByte(aIndex);
            byte vb = smaller[bIndex];
            if (va != vb) {
                return false;
            }
            aIndex ++;
            bIndex ++;
        }

        return true;
	}

}
