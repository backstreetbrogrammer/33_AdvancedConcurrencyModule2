package com.backstreetbrogrammer.ch01_locksSemaphores;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StudentTimedLock {

    public final Lock lock = new ReentrantLock();

    public void admit() {
        if (lock.tryLock()) {
            try {
                // code for admission
            } finally {
                lock.unlock();
            }
        } else {
            // do something else
        }
    }

    public void admit(final long timeToTry) {
        try {
            if (lock.tryLock(timeToTry, TimeUnit.SECONDS)) {
                try {
                    // code for admission
                } finally {
                    lock.unlock();
                }
            } else {
                // do something else
            }
        } catch (final InterruptedException e) {
            // if interrupted, release the waiting thread and do something else
            e.printStackTrace();
        }
    }

}
