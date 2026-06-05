package org.example;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadSafeCache<K,V> {
    private final ConcurrentHashMap<K,CacheEntry<V>>map;
    private final ScheduledExecutorService cleanupExecutor;
    ThreadSafeCache(long TTL){
        this.map=new ConcurrentHashMap<>();
        if(TTL>0){
            this.cleanupExecutor= Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "cache-cleanup");
                t.setDaemon(true);
                return t;
            });
            cleanupExecutor.scheduleAtFixedRate(
                    this::removeExpiredEntries,
                    TTL,
                    TTL,
                    TimeUnit.MILLISECONDS
            );
        }else{
            this.cleanupExecutor=null;
        }
    }

    public V get(K key){
        CacheEntry<V> entry=map.get(key);
        if (entry == null) return null;
        if(entry.isExpired()){
            map.remove(key,entry);
            return null;
        }

        return entry.getValue();
    }

    public void put(K key, V value, long ttlMs){
        CacheEntry<V> entry=new CacheEntry<>(value, ttlMs);
        map.put(key,entry);
    }

    public boolean remove(K key){
        return map.remove(key)!=null;
    }

    public void removeExpiredEntries(){
        for(K key:map.keySet()){
            CacheEntry<V> entry=map.get(key);
            if(entry!=null&&entry.isExpired()){
                map.remove(key,entry);
            }
        }
    }
    public int size() {
        // Note: this includes potentially expired entries
        return map.size();
    }

    public int activeSize() {
        // Count only non-expired entries
        return (int) map.values().stream()
                .filter(entry -> !entry.isExpired())
                .count();
    }
    public void shutdown(){
        if(cleanupExecutor!=null){
            cleanupExecutor.shutdown();
            try{
                cleanupExecutor.awaitTermination(5,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        map.clear();
    }
}
