package com.backstreetbrogrammer.ch01_locksSemaphores.semaphore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class BoundedBufferTest {

    private BoundedBuffer<Integer> buffer;

    @BeforeEach
    void setUp() {
        buffer = new BoundedBuffer<>(5);
    }

    @Test
    @DisplayName("Test buffer is empty when constructed")
    void testBufferIsEmptyWhenConstructed() {
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
    }

    @Test
    @DisplayName("Test buffer is full after all puts")
    void testBufferIsFullAfterPuts() throws InterruptedException {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(i);
        }
        assertTrue(buffer.isFull());
        assertFalse(buffer.isEmpty());
    }

    @Test
    @DisplayName("Test buffer is full after all puts and then empty after all takes")
    void testBufferIsFullAfterPutsAndIsEmptyAfterTakes() throws InterruptedException {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(i);
        }
        assertTrue(buffer.isFull());
        assertFalse(buffer.isEmpty());

        for (int i = 0; i < buffer.capacity(); i++) {
            assertEquals(i, buffer.take());
        }
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
    }

    @Test
    @DisplayName("Test multithreaded puts and takes")
    void testMultiThreadedPutsAndTakes() throws InterruptedException {
        final int putMax = 10;
        final int takeMax = 7;

        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < putMax; i++) {
                try {
                    buffer.put(i);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < takeMax; i++) {
                try {
                    buffer.take();
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        
        TimeUnit.SECONDS.sleep(2L);

        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(putMax - takeMax, buffer.size());

        for (int i = takeMax; i < putMax; i++) {
            assertEquals(i, buffer.take());
        }
    }

}
