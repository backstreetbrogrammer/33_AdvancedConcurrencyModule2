package com.backstreetbrogrammer.ch01_locksSemaphores.readWriteLock;

public interface JReadWriteLockI {

    void lockRead() throws InterruptedException;

    void unlockRead();

    void lockWrite() throws InterruptedException;

    void unlockWrite();

}
