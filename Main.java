package org.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Create cache with 1-second cleanup interval
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(1000);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        AtomicInteger hits = new AtomicInteger(0);
        AtomicInteger misses = new AtomicInteger(0);

        // Writer threads: continuously put entries with short TTL
        for (int i = 0; i < 3; i++) {
            final int writerId = i;
            executor.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    String key = "key-" + (j % 10);  // 10 unique keys
                    String value = "writer-" + writerId + "-iteration-" + j;
                    cache.put(key, value, 100);  // 100ms TTL

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }

        // Reader threads: continuously read entries
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                for (int j = 0; j < 200; j++) {
                    String key = "key-" + (j % 10);
                    String value = cache.get(key);

                    if (value != null) {
                        hits.incrementAndGet();
                    } else {
                        misses.incrementAndGet();
                    }

                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }

        executor.shutdown();
        try{
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        System.out.println("Cache hits: " + hits.get());
        System.out.println("Cache misses: " + misses.get());
        System.out.println("Final cache size: " + cache.size());
        System.out.println("Final active size: " + cache.activeSize());

        // Demonstrate TTL expiration
        System.out.println("\n--- TTL Expiration Demo ---");
        cache.put("session", "user-123", 500);  // 500ms TTL
        System.out.println("Put 'session' with 500ms TTL");
        System.out.println("Immediate get: " + cache.get("session"));

        Thread.sleep(600);
        System.out.println("After 600ms: " + cache.get("session"));  // Should be null

        cache.shutdown();

    }
}