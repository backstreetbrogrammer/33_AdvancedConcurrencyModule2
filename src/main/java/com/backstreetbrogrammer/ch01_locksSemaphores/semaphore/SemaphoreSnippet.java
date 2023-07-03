package com.backstreetbrogrammer.ch01_locksSemaphores.semaphore;

import java.util.concurrent.Semaphore;

public class SemaphoreSnippet {

    public static void main(final String[] args) {
        final Semaphore semaphore = new Semaphore(5); // permits
        try {
            semaphore.acquire();
            // guarded block of code
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }
}
