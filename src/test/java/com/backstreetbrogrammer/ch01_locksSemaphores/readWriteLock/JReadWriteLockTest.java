package com.backstreetbrogrammer.ch01_locksSemaphores.readWriteLock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JReadWriteLockTest {

    private final JReadWriteLockI readWriteLock = new JReadWriteLockFullyReentrant();
    private int counter = 0;

    @Test
    @DisplayName("Test ReadWriteLock implementations")
    void testReadWriteLock() throws InterruptedException {
        final var reader1 = getReaderThread("Reader1", readWriteLock);
        final var reader2 = getReaderThread("Reader2", readWriteLock);
        final var reader3 = getReaderThread("Reader3", readWriteLock);
        final var reader4 = getReaderThread("Reader4", readWriteLock);
        final var reader5 = getReaderThread("Reader5", readWriteLock);

        final var writer1 = getWriterThread("Writer1", readWriteLock);
        final var writer2 = getWriterThread("Writer2", readWriteLock);

        reader1.start();
        writer1.start();
        reader2.start();
        reader3.start();
        writer2.start();
        reader4.start();

        TimeUnit.SECONDS.sleep(1L);

        reader5.start();
        assertEquals(2, counter);

        System.out.println("--------------------\n");
    }

    private void incrementCounter(final JReadWriteLockI readWriteLock) throws InterruptedException {
        readWriteLock.lockWrite();
        try {
            counter++;
        } finally {
            readWriteLock.unlockWrite();
        }
    }

    private int getCounter(final JReadWriteLockI readWriteLock) throws InterruptedException {
        readWriteLock.lockRead();
        try {
            return counter;
        } finally {
            readWriteLock.unlockRead();
        }
    }

    private Thread getReaderThread(final String name, final JReadWriteLockI readWriteLock) {
        return new Thread(() -> {
            try {
                final int counter = getCounter(readWriteLock);
                System.out.printf("Time:[%s], Reader Thread: [%s], Counter: [%d]%n", LocalDateTime.now(),
                                  Thread.currentThread().getName(), counter);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }, name);
    }

    private Thread getWriterThread(final String name, final JReadWriteLockI readWriteLock) {
        return new Thread(() -> {
            try {
                incrementCounter(readWriteLock);
                System.out.printf("Time:[%s], Writer Thread: [%s]%n", LocalDateTime.now(),
                                  Thread.currentThread().getName());
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }, name);
    }
}
