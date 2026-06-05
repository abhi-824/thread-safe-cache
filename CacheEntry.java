package org.example;

import java.util.concurrent.TimeUnit;

public class CacheEntry<V> {
    private final V value;
    private final long expirationTime;

    CacheEntry(V value, long expirationTime){
        this.value=value;
        this.expirationTime=System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(expirationTime);
    }

    V getValue(){
        return this.value;
    }

    long getExpirationTime(){
        return this.expirationTime;
    }

    boolean isExpired(){
        return System.nanoTime()>expirationTime;
    }
}
