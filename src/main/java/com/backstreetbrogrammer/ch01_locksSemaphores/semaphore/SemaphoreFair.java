package com.backstreetbrogrammer.ch01_locksSemaphores.semaphore;

import java.util.concurrent.Semaphore;

public class SemaphoreFair {

    public static void main(final String[] args) {
        final Semaphore semaphore = new Semaphore(5, true); // fair
        try {
            semaphore.acquire(2);
            // guarded block of code
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release(2);
        }
    }
}
