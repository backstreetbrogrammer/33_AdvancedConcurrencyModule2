package com.backstreetbrogrammer.ch02_barriersLatches.latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Service extends Thread {

    private final int delay;
    private final CountDownLatch latch;

    public Service(final String name, final int delay, final CountDownLatch latch) {
        super.setName(name);
        this.delay = delay;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
            latch.countDown();
            System.out.printf("%s initialized%n", Thread.currentThread().getName());
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }
}
