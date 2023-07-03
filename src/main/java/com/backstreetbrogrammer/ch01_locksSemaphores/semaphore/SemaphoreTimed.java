package com.backstreetbrogrammer.ch01_locksSemaphores.semaphore;

import java.util.concurrent.Semaphore;

public class SemaphoreTimed {

    public static void main(final String[] args) {
        final Semaphore semaphore = new Semaphore(5); // permits
        try {
            if (semaphore.tryAcquire()) {
                // guarded block of code
            } else {
                // do something else
            }
        } finally {
            semaphore.release();
        }
    }
}
