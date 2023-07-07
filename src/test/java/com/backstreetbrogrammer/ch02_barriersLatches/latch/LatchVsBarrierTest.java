package com.backstreetbrogrammer.ch02_barriersLatches.latch;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LatchVsBarrierTest {

    @Test
    void testLatch() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final Thread t = new Thread(() -> {
            countDownLatch.countDown();
            countDownLatch.countDown();
        });
        t.start();
        countDownLatch.await();

        assertEquals(0, countDownLatch.getCount());
    }

    @Test
    void testBarrier() {
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
        final Thread t = new Thread(() -> {
            try {
                cyclicBarrier.await();
                cyclicBarrier.await();
            } catch (final InterruptedException | BrokenBarrierException e) {
                // error handling
            }
        });
        t.start();

        assertFalse(cyclicBarrier.isBroken());
    }
}
