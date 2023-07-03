package com.backstreetbrogrammer.ch01_locksSemaphores.semaphore;

import java.util.concurrent.Semaphore;

public class SemaphoreUninterruptible {

    public static void main(final String[] args) {
        final Semaphore semaphore = new Semaphore(5); // permits
        try {
            semaphore.acquireUninterruptibly();
            // guarded block of code
        } finally {
            semaphore.release();
        }
    }
}
