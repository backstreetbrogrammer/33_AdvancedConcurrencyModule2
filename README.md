# Advanced Concurrency - Module 2

> This is a tutorials course covering advanced concurrency in Java.

Tools used:

- JDK 11
- Maven
- JUnit 5, Mockito
- IntelliJ IDE

## Table of contents

1. [Advanced Locking and Semaphores](https://github.com/backstreetbrogrammer/33_AdvancedConcurrencyModule2#chapter-01-advanced-locking-and-semaphores)
    - [Lock pattern](https://github.com/backstreetbrogrammer/33_AdvancedConcurrencyModule2#lock-pattern)
    - [Condition](https://github.com/backstreetbrogrammer/33_AdvancedConcurrencyModule2#condition)
    - [Read-Write Locks](https://github.com/backstreetbrogrammer/33_AdvancedConcurrencyModule2#read-write-locks)
    - [Semaphore Pattern](https://github.com/backstreetbrogrammer/33_AdvancedConcurrencyModule2#semaphore-pattern)
2. [Using Barriers and Latches](https://github.com/backstreetbrogrammer/33_AdvancedConcurrencyModule2#chapter-02-using-barriers-and-latches)

---

### Chapter 01. Advanced Locking and Semaphores

There are 2 ways of using **intrinsic** locking: `synchronization` and `volatile`.

```java
public class Student {

    public final Object lock = new Object();

    public void admit() {
        synchronized (lock) {
            // code for admission
        }
    }

}
```

This code prevents more than one thread to execute the `synchronized` block at the same time.

When several threads are trying to execute `admit()` => only 1 of them will be allowed to execute, all other threads
will have to **wait** for their turn (in the lock object's wait-queue).

What happens when the 1 thread executing the `admit()` method gets **blocked** inside the method (due to some bug)?

All the threads and the executing thread are **BLOCKED** => there is **NO** way to release them. ONLY way is to restart
the JVM!

#### Lock pattern

The **LOCK pattern** comes here for rescue.

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Student {

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
```

Now whatever exception is thrown by the executing thread in the critical section (code for admission), `finally` block
will ensure to **RELEASE** the lock.

`Lock` is an interface, implemented by `ReentrantLock`. It offers the same guarantees as **mutual exclusion**, **read &
write ordering**, etc.

**Benefits of Lock pattern**

We have explicit control over the `Lock` object which defines several methods, in contrast to `Object` lock used in
`synchronized` block which only has `wait()`, `notify()` and `notifyAll()` methods.

- **Interruptible lock acquisition**

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Student {

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
```

The threads waiting to acquire the lock => if they are interrupted by another thread (by calling `interrupt()` method on
that waiting threads references) - they will throw the `InterruptedException`.

Now the waiting threads are released and can be used for other work or again wait => developer is free to use it.

This pattern is useful to kill a contingent of threads in a pool which are all waiting to acquire a lock.

- **Timed lock acquisition**

```java
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
```

If another thread is already executing the critical section, then `tryLock()` will return `false` immediately and
instead of waiting => other threads can do something else.

Overloaded version of `tryLock(2L, TimeUnit.SECONDS)` (example) can be used to wait for allotted time to try acquiring
the lock before returning `false`.

Both `lockInterruptibly()` and `tryLock()` methods in `Lock` can help avoid any **DEADLOCK** situation as the waiting
threads do NOT need to wait indefinitely and can be intervened and released.

- **Fair lock acquisition**

While using **intrinsic** locking by `synchronized` block or using **explicit** locking by `Lock` pattern, the **first**
thread to enter the guarded block of code is chosen **randomly**.

**Fairness** means that the **first** thread to enter the wait line should be the **first** thread to enter the block of
code.

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StudentFair {

    public final Lock lock = new ReentrantLock(true); // fair

    public void admit() {
        try {
            lock.lock();
            // code for admission
        } finally {
            lock.unlock();
        }
    }

}
```

By passing the boolean argument as `true` to `ReentrantLock` object constructor => fairness is guaranteed.

It means if 2 threads are waiting to execute the `admit()` method => the first thread to enter the wait queue will be
executed first.

**Cons**: A fair lock is costly (required more CPU / memory resources) => that's why default behavior is non-fair.

#### Interview Problem 1 (Macquarie): Explain and Implement Producer Consumer pattern using Locks

In the basic concurrency module, we learnt how to implement Producer-Consumer pattern using `wait()`, `notify()`
and `notifyAll()` methods.

Just to revise:

```java
public class ProducerConsumerUsingWaitNotify {
    private static final Object lock = new Object();

    private static int[] buffer;
    private static int count;

    private static class Producer {
        void produce() {
            synchronized (lock) {
                while (isFull(buffer)) {
                    try {
                        lock.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                buffer[count++] = 1;
                lock.notifyAll();
            }
        }
    }

    private static class Consumer {
        void consume() {
            synchronized (lock) {
                while (isEmpty()) {
                    try {
                        lock.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                buffer[--count] = 0;
                lock.notifyAll();
            }
        }
    }

    private static boolean isEmpty() {
        return count == 0;
    }

    private static boolean isFull(final int[] buffer) {
        return count == buffer.length;
    }
}
```

This implementation is good and correct. However, it has one limitation - say, if the buffer is empty and consumer
thread is waiting on the object lock - it has to keep on waiting until a producer thread writes the data to buffer and
calls `notifyAll()`. There is no way that consumer thread can be moved out of waiting state unless we restart the JVM.

We can use `Lock` and corresponding 2 `Condition` objects on **full** or **empty** conditions to separate out wait and
notify calls.

#### Condition

- A `Condition` object is used to park and awake threads same as wait and notify.
- A `Lock` object can have any number of `Condition` objects.
- A **fair** `Lock` generates **fair** `Condition`
- A `Condition` object extends `Object`, so it has `wait()` and `notify()` methods. However, it will not work correctly
  as it may not be under synchronized block.
- The `await()` call is blocking but it can be interrupted.

There are five versions for `await` method:

- await()
- await(time, timeUnit)
- awaitNanos(nanosTimeout)
- awaitUntil(date)
- awaitUninterruptibly()

These are ways to prevent the blocking of waiting threads with the `Condition` API.

Based on all this, here is our final version of Producer-Consumer Pattern using `Lock` and `Condition`.

```java
import java.util.concurrent.TimeUnit;
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
                    notFull.await();
                }
                buffer[count++] = 1;
                notEmpty.signalAll();
            } catch (final InterruptedException e) {
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
                    notEmpty.await();
                }
                buffer[--count] = 0;
                notFull.signalAll();
            } catch (final InterruptedException e) {
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
```

Sample output:

```
Done consuming: Consumer3
Done consuming: Consumer1
Done producing: Producer1
Done consuming: Consumer2
Done producing: Producer2
Data in the buffer: 5
```

#### Read-Write Locks

#### Semaphore Pattern

---

### Chapter 02. Using Barriers and Latches

---
