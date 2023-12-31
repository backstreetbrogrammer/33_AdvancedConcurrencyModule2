package com.backstreetbrogrammer.ch01_locksSemaphores.readWriteLock;

import java.util.HashMap;
import java.util.Map;

public class JReadWriteLockFullyReentrant implements JReadWriteLockI {

    private final Map<Thread, Integer> readingThreads = new HashMap<>();
    private int writeAccesses;
    private int writeRequests;
    private Thread writingThread;

    @Override
    public synchronized void lockRead() throws InterruptedException {
        final var callingThread = Thread.currentThread();
        while (!canGrantReadAccess(callingThread)) {
            wait();
        }
        readingThreads.merge(callingThread, 1, Integer::sum);
    }

    @Override
    public synchronized void unlockRead() {
        final var callingThread = Thread.currentThread();
        final int accessCount = readingThreads.get(callingThread);
        if (accessCount == 1) {
            readingThreads.remove(callingThread);
        } else {
            readingThreads.put(callingThread, accessCount - 1);
        }
        notifyAll();
    }

    @Override
    public synchronized void lockWrite() throws InterruptedException {
        writeRequests++;
        final var callingThread = Thread.currentThread();
        final var readers = readingThreads.get(callingThread);
        while (!canGrantWriteAccess(callingThread) || ((readers != null) && (readers > 0))) {
            wait();
        }
        writeRequests--;
        writeAccesses++;
        writingThread = callingThread;
    }

    @Override
    public synchronized void unlockWrite() {
        writeAccesses--;
        if (writeAccesses == 0) {
            writingThread = null;
        }
        notifyAll();
    }

    private boolean canGrantWriteAccess(final Thread callingThread) {
        if (readingThreads.size() == 1 && readingThreads.containsKey(callingThread)) return true;
        return (writingThread == null) || (writingThread == callingThread);
    }

    private boolean canGrantReadAccess(final Thread callingThread) {
        if ((writingThread == null) || (writingThread == callingThread)) return true;
        if (readingThreads.containsKey(callingThread)) return true;
        return writeRequests <= 0;
    }

}
