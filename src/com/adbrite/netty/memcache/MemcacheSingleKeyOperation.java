package com.adbrite.netty.memcache;


public abstract class MemcacheSingleKeyOperation extends MemcacheOperation {
	abstract NormalizedKey getKey();
}
