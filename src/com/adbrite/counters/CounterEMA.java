package com.adbrite.counters;

import java.util.concurrent.atomic.AtomicLong;

/**
 * exponentially weighted moving average
 *
 * @author apesternikov
 *
 */
public class CounterEMA {
	private static final double DEFAULT_W = 0.01;
	private final double W;
	private final AtomicLong avgLongBits;
	
	/**
	 * Create EMA counter with specified averaging weight
	 * @param weight
	 */
	public CounterEMA(double weight) {
		W = weight;
		avgLongBits = new AtomicLong(Double.doubleToLongBits(0.0));
	}
	
	/**
	 * Create EMA counter with default averaging weight
	 */
	public CounterEMA() {
		this(DEFAULT_W);
	}
	
	public void add(double value) {
		long longbits;
		long newlongbits;
		do {
			longbits = avgLongBits.get();
			double avg = Double.longBitsToDouble(longbits);
			avg = (1-W)*avg + W*value;
			newlongbits = Double.doubleToLongBits(avg);
		}while(!avgLongBits.compareAndSet(longbits, newlongbits));
	}
	
	public double get() {
		return Double.longBitsToDouble(avgLongBits.get());
	}

}
