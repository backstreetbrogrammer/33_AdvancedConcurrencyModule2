package com.backstreetbrogrammer.ch01_locksSemaphores;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StudentLock {

    public final Lock lock = new ReentrantLock();

    public void admit() {
        try {
            lock.lock();
            // code for admission
        } finally {
            lock.unlock();
        }
    }

}
