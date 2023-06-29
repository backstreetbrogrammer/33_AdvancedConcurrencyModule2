package com.backstreetbrogrammer.ch01_locksSemaphores;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StudentInterruptibleLock {

    public final Lock lock = new ReentrantLock();

    public void admit() {
        try {
            lock.lockInterruptibly();
            // code for admission
        } catch (final InterruptedException e) {
            // if interrupted, release the waiting thread and do something else
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

}
