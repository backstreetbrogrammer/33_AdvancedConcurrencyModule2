package com.backstreetbrogrammer.ch01_locksSemaphores.semaphore;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class BoundedBuffer<E> {

    private final Semaphore availableItems, availableSpaces;
    private final E[] items;
    private int putPosition = 0, takePosition = 0;

    public BoundedBuffer(final int capacity) {
        availableItems = new Semaphore(0);
        availableSpaces = new Semaphore(capacity);
        items = (E[]) new Object[capacity];
    }

    public boolean isEmpty() {
        return availableItems.availablePermits() == 0;
    }

    public boolean isFull() {
        return availableSpaces.availablePermits() == 0;
    }

    public E[] getItems() {
        return Arrays.copyOf(items, items.length);
    }

    public int capacity() {
        return items.length;
    }

    public int size() {
        int count = 0;
        for (final E item : items) {
            if (item != null) {
                count++;
            }
        }
        return count;
    }

    public void put(final E item) throws InterruptedException {
        availableSpaces.acquire();
        doInsert(item);
        availableItems.release();
    }

    public E take() throws InterruptedException {
        availableItems.acquire();
        final E item = doExtract();
        availableSpaces.release();
        return item;
    }

    private synchronized void doInsert(final E item) {
        int i = putPosition;
        items[i] = item;
        putPosition = (++i == items.length) ? 0 : i;
    }

    private synchronized E doExtract() {
        int i = takePosition;
        final E item = items[i];
        items[i] = null;
        takePosition = (++i == items.length) ? 0 : i;
        return item;
    }

}
