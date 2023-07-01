package com.backstreetbrogrammer.ch01_locksSemaphores.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiThreadedHashMap<K, V> {
    private final Map<K, V> cache = new HashMap<>();

    private final ReadWriteLock readWritelock = new ReentrantReadWriteLock();
    private final Lock readLock = readWritelock.readLock();
    private final Lock writeLock = readWritelock.writeLock();

    // GET - guard with read lock
    public V get(final K key) {
        try {
            readLock.lock();
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }

    // PUT - guard with write lock
    public V put(final K key, final V value) {
        try {
            writeLock.lock();
            return cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    // REMOVE - guard with write lock
    public V remove(final K key) {
        try {
            writeLock.lock();
            return cache.remove(key);
        } finally {
            writeLock.unlock();
        }
    }

}
