package com.backstreetbrogrammer.ch01_locksSemaphores.producerConsumer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumerUsingLocks {

    private static final Lock lock = new ReentrantLock();
    private static final Condition notFull = lock.newCondition();
    private static final Condition notEmpty = lock.newCondition();

    private static int[] buffer;
    private static int count;

    private static class Producer {
        void produce() {
            try {
                lock.lock();
                while (isFull(buffer)) {
                    if (!notFull.await(10, TimeUnit.MILLISECONDS)) {
                        throw new TimeoutException("Producer time out!");
                    }
                }
                buffer[count++] = 1;
                notEmpty.signalAll();
            } catch (final InterruptedException | TimeoutException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    private static class Consumer {
        void consume() {
            try {
                lock.lock();
                while (isEmpty()) {
                    if (!notEmpty.await(10, TimeUnit.MILLISECONDS)) {
                        throw new TimeoutException("Consumer time out!");
                    }
                }
                buffer[--count] = 0;
                notFull.signalAll();
            } catch (final InterruptedException | TimeoutException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    private static boolean isEmpty() {
        return count == 0;
    }

    private static boolean isFull(final int[] buffer) {
        return count == buffer.length;
    }

    private static Runnable createProducerTask(final Producer producer, final int num, final String name) {
        return () -> {
            for (int i = 0; i < num; i++) {
                producer.produce();
            }
            System.out.printf("Done producing: %s%n", name);
        };
    }

    private static Runnable createConsumerTask(final Consumer consumer, final int num, final String name) {
        return () -> {
            for (int i = 0; i < num; i++) {
                consumer.consume();
            }
            System.out.printf("Done consuming: %s%n", name);
        };
    }

    public static void main(final String... strings) throws InterruptedException {
        buffer = new int[10];
        count = 0;

        final Thread[] producerThreads = new Thread[]{
                new Thread(createProducerTask(new Producer(), 30, "Producer1")),
                new Thread(createProducerTask(new Producer(), 20, "Producer2"))
        };
        final Thread[] consumerThreads = new Thread[]{
                new Thread(createConsumerTask(new Consumer(), 20, "Consumer1")),
                new Thread(createConsumerTask(new Consumer(), 15, "Consumer2")),
                new Thread(createConsumerTask(new Consumer(), 10, "Consumer3"))
        };

        for (final Thread producer : producerThreads) {
            producer.start();
        }
        for (final Thread consumer : consumerThreads) {
            consumer.start();
        }

        TimeUnit.SECONDS.sleep(1L);

        for (final Thread consumer : consumerThreads) {
            consumer.join();
        }
        for (final Thread producer : producerThreads) {
            producer.join();
        }

        System.out.printf("Data in the buffer: %d%n", count);
    }

}
