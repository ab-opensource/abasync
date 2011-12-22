package com.adbrite.netty;

public interface OpExecutionStats {
	void registerSuccess(long executionTime);
	void registerFailure(long executionTime, Throwable failureReason);
}
