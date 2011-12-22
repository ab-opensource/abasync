package com.adbrite.counters;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * A lock-free circular buffer of longs.
 * When the buffer is full, the oldest item is overwritten.
 *
 */
public class CircularBuffer {
    
    private final AtomicLong index = new AtomicLong(-1);
    private final AtomicLongArray buffer;
    private final long size;
    
    public CircularBuffer() {
    	this(100);
	}

    public CircularBuffer(int size) {
        this.size = size;
        buffer = new AtomicLongArray(size);
    }
    
    public long put(long item) {
    	long idx = index.incrementAndGet();
    	int i = (int)(idx%size);
    	long oldval = buffer.getAndSet(i, item);
    	return oldval;
    }

    /**
     * Get contents of buffer, as an array.
     * The ordering is unpredictable.
     */
    public long[] getAll() {
    	long[] ret = new long[(int)size];
        for (int i = 0; i < size; i++) 
        	ret[i]=buffer.get(i);
        return ret;
    }
   
}
