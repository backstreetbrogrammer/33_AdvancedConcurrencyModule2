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

Imagine we have an application that **reads** and **writes** some resource, but **writing** is not done as much as
**reading**.

Multiple threads **reading** the same resource does not cause problems for each other, so multiple threads that want to
**read** the resource are granted access at the same time, overlapping.

But, if a single thread wants to **write** to the resource, no other **reads** nor **writes** must be in progress at the
same time.

To solve this problem of allowing **multiple readers** but **only one writer**, we will need a read / write lock.

To summarize the conditions for getting read and write access to the resource:

**Read Access** - If no threads are writing, and no threads have requested write access.

**Write Access** - If no threads are reading or writing.

**ReadWriteLock**

`ReadWriteLock` is an interface with only two methods:

```java
public interface ReadWriteLock {
    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading
     */
    Lock readLock();

    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing
     */
    Lock writeLock();
}
```

- Only **one thread** can hold the **write** lock
- When the **write** lock is held, no one can hold the **read** lock
- As many threads as needed can hold the **read** lock

#### Interview Problem 2 (Barclays): Implement multi-threaded cache using Read-Write Locks

Design a multi-threaded cache which will guarantee thread safe operations for `get()`, `put()` and `remove()` operations
using Read-Write locks.

**NOTE**: we can use Java `HashMap` and make it thread safe using `ReadWriteLock`. Same functionality can be done using
`ConcurrentHashMap` but candidate should use `HashMap` for this problem.

**Solution**:

We can use `ReadWriteLock` to make all the map operations thread-safe.

```java
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiThreadedHashMap<K, V> {
    private final Map<K, V> cache = new HashMap<>();

    private final ReadWriteLock readWritelock = new ReentrantReadWriteLock();
    private final Lock readLock = readWritelock.readLock();
    private final Lock writeLock = readWritelock.writeLock();

    // GET - guard with read lock
    public V get(final K key) {
        try {
            readLock.lock();
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }

    // PUT - guard with write lock
    public V put(final K key, final V value) {
        try {
            writeLock.lock();
            return cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    // REMOVE - guard with write lock
    public V remove(final K key) {
        try {
            writeLock.lock();
            return cache.remove(key);
        } finally {
            writeLock.unlock();
        }
    }

}
```

Unit Test class:

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MultiThreadedHashMapTest {

    private MultiThreadedHashMap<String, Integer> cache;

    @BeforeEach
    void setUp() {
        cache = new MultiThreadedHashMap<>();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Rishi", "John", "Bob", "Malcolm", "Joshua"})
    @DisplayName("Test put() and get() methods with one input at a time")
    void testPutAndGetMethodsWithOneInputAtATime(final String input) {
        cache.put(input, input.length());
        final int value = cache.get(input);
        assertEquals(input.length(), value);
    }

    @Test
    @DisplayName("Test put() and get() methods with multiple inputs")
    void testPutAndGetMethodsWithMultipleInputs() {
        final String[] inputs = new String[]{"Rishi", "John", "Bob", "Malcolm", "Joshua"};
        for (final String input : inputs) {
            cache.put(input, input.length());
            final int value = cache.get(input);
            assertEquals(input.length(), value);
        }
        cache.put("Bob", 10);
        assertEquals(10, cache.get("Bob"));
    }

    @Test
    @DisplayName("Test put() and remove() methods with multiple inputs")
    void testPutAndRemoveMethodsWithMultipleInputs() {
        final String[] inputs = new String[]{"Rishi", "John", "Bob", "Malcolm", "Joshua", "Christy"};
        for (final String input : inputs) {
            cache.put(input, input.length());
            final int value = cache.get(input);
            assertEquals(input.length(), value);
        }
        cache.remove("Bob");
        assertNull(cache.get("Bob"));
    }

    @Test
    @DisplayName("Test remove() method from head in collided index")
    void testRemoveMethodFromHead() {
        final String[] inputs = new String[]{"Rishi", "John", "Bob", "Malcolm", "Joshua", "Christy"};
        for (final String input : inputs) {
            cache.put(input, input.length());
            final int value = cache.get(input);
            assertEquals(input.length(), value);
        }
        cache.remove("Christy");
        assertNull(cache.get("Christy"));
    }

    @Test
    @DisplayName("Test remove() method from tail in collided index")
    void testRemoveMethodFromTail() {
        final String[] inputs = new String[]{"Rishi", "John", "Bob", "Malcolm", "Joshua", "Christy"};
        for (final String input : inputs) {
            cache.put(input, input.length());
            final int value = cache.get(input);
            assertEquals(input.length(), value);
        }
        cache.remove("Rishi");
        assertNull(cache.get("Rishi"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Rishi", "John", "Bob", "Malcolm", "Joshua"})
    @DisplayName("When parallel computation is applied on cache, then results are correct and consistent")
    public void whenParallelComputationAppliedToCache_thenCorrectAndConsistentResults(final String input) throws Exception {
        cache.put(input, input.length());
        final List<Integer> sumList = parallelComputation(input, 1000);

        assertEquals(1, sumList
                .stream()
                .distinct()
                .count());

        final long wrongResultCount = sumList
                .stream()
                .filter(num -> num != input.length())
                .count();

        assertEquals(0, wrongResultCount);
    }

    private List<Integer> parallelComputation(final String str,
                                              final int executionTimes) throws InterruptedException {
        final List<Integer> sumList = new CopyOnWriteArrayList<>();
        for (int i = 0; i < executionTimes; i++) {
            final ExecutorService executorService = Executors.newFixedThreadPool(4);
            for (int j = 0; j < 10; j++) {
                executorService.execute(() -> {
                    for (int k = 0; k < 10; k++) {
                        sumList.add(cache.get(str));
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            sumList.add(cache.get(str));
        }
        return sumList;
    }

}
```

#### Interview Problem 3 (Barclays): Implement custom Read-Write Lock

Implement custom Read-Write lock which should be reentrant and thread safe.

**Solution**:

We will define an interface first.

```java
public interface JReadWriteLockI {

    void lockRead() throws InterruptedException;

    void unlockRead();

    void lockWrite() throws InterruptedException;

    void unlockWrite();

}
```

We implement the interface as fully reentrant and thread safe read-write lock.

```java
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
```

Unit Test class:

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JReadWriteLockTest {

    private final JReadWriteLockI readWriteLock = new JReadWriteLockFullyReentrant();
    private int counter = 0;

    @Test
    @DisplayName("Test ReadWriteLock implementations")
    void testReadWriteLock() throws InterruptedException {
        final var reader1 = getReaderThread("Reader1", readWriteLock);
        final var reader2 = getReaderThread("Reader2", readWriteLock);
        final var reader3 = getReaderThread("Reader3", readWriteLock);
        final var reader4 = getReaderThread("Reader4", readWriteLock);
        final var reader5 = getReaderThread("Reader5", readWriteLock);

        final var writer1 = getWriterThread("Writer1", readWriteLock);
        final var writer2 = getWriterThread("Writer2", readWriteLock);

        reader1.start();
        writer1.start();
        reader2.start();
        reader3.start();
        writer2.start();
        reader4.start();

        TimeUnit.SECONDS.sleep(1L);

        reader5.start();
        assertEquals(2, counter);

        System.out.println("--------------------\n");
    }

    private void incrementCounter(final JReadWriteLockI readWriteLock) throws InterruptedException {
        readWriteLock.lockWrite();
        try {
            counter++;
        } finally {
            readWriteLock.unlockWrite();
        }
    }

    private int getCounter(final JReadWriteLockI readWriteLock) throws InterruptedException {
        readWriteLock.lockRead();
        try {
            return counter;
        } finally {
            readWriteLock.unlockRead();
        }
    }

    private Thread getReaderThread(final String name, final JReadWriteLockI readWriteLock) {
        return new Thread(() -> {
            try {
                final int counter = getCounter(readWriteLock);
                System.out.printf("Time:[%s], Reader Thread: [%s], Counter: [%d]%n", LocalDateTime.now(),
                                  Thread.currentThread().getName(), counter);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }, name);
    }

    private Thread getWriterThread(final String name, final JReadWriteLockI readWriteLock) {
        return new Thread(() -> {
            try {
                incrementCounter(readWriteLock);
                System.out.printf("Time:[%s], Writer Thread: [%s]%n", LocalDateTime.now(),
                                  Thread.currentThread().getName());
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }, name);
    }
}
```

Sample output:

```
Time:[2023-07-02T07:40:57.292520600], Reader Thread: [Reader1], Counter: [0]
Time:[2023-07-02T07:40:57.301497100], Reader Thread: [Reader4], Counter: [0]
Time:[2023-07-02T07:40:57.301497100], Writer Thread: [Writer2]
Time:[2023-07-02T07:40:57.300499200], Reader Thread: [Reader2], Counter: [1]
Time:[2023-07-02T07:40:57.299502300], Writer Thread: [Writer1]
Time:[2023-07-02T07:40:57.299502300], Reader Thread: [Reader3], Counter: [2]
Time:[2023-07-02T07:40:58.272740900], Reader Thread: [Reader5], Counter: [2]
--------------------
```

#### Semaphore Pattern

**Semaphore** is similar to **locks** but instead of allowing only **mutual exclusions** (one thread) - it can permit
**several** threads to enter the **critical section**.

```
        final Semaphore semaphore = new Semaphore(5); // permits
        try {
            semaphore.acquire();
            // guarded block of code
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
```

The `acquire()` call is **blocking** until a permit is available - at most 5 threads can execute the guarded code at the
same time. By default, semaphore is **non-fair**.

Semaphore can be created as **fair**

```
        final Semaphore semaphore = new Semaphore(5, true); // fair
        try {
            semaphore.acquire(2);
            // guarded block of code
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release(2);
        }
```

The `acquire()` can ask for more than one permit - then the `release()` call **MUST** release them all.

By default, if a waiting thread is interrupted it will throw an `InterruptedException` - that's why we have a catch
block to handle it.

```
        final Semaphore semaphore = new Semaphore(5); // permits
        try {
            semaphore.acquireUninterruptibly();
            // guarded block of code
        } finally {
            semaphore.release();
        }
```

**Uninterruptibility** means that the thread cannot be interrupted => it can be only be freed by calling its
`release()` method.

Similar to `tryLock()` in `Lock`, there is a timed version for `Semaphore` too:

```
        final Semaphore semaphore = new Semaphore(5); // permits
        try {
            if (semaphore.tryAcquire()) { // or, semaphore.tryAcquire(1, TimeUnit.SECONDS)
                // guarded block of code
            } else {
                // do something else
            }
        } finally {
            semaphore.release();
        }
```

All of these methods can also request more than one permit and accordingly, must release it.

**Handling Permits**

One can reduce the number of permits (cannot increase it)

**Waiting Threads**

One can check the waiting threads:

- are there any waiting threads?
- how many threads are waiting?
- get the collection of the waiting threads

#### Interview Problem 4 (Morgan Stanley): Implement a bounded buffer using Semaphore

Design a thread-safe bounded buffer using Semaphore.

**Solution**:

```java
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
```

Unit Test:

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class BoundedBufferTest {

    private BoundedBuffer<Integer> buffer;

    @BeforeEach
    void setUp() {
        buffer = new BoundedBuffer<>(5);
    }

    @Test
    @DisplayName("Test buffer is empty when constructed")
    void testBufferIsEmptyWhenConstructed() {
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
    }

    @Test
    @DisplayName("Test buffer is full after all puts")
    void testBufferIsFullAfterPuts() throws InterruptedException {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(i);
        }
        assertTrue(buffer.isFull());
        assertFalse(buffer.isEmpty());
    }

    @Test
    @DisplayName("Test buffer is full after all puts and then empty after all takes")
    void testBufferIsFullAfterPutsAndIsEmptyAfterTakes() throws InterruptedException {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put(i);
        }
        assertTrue(buffer.isFull());
        assertFalse(buffer.isEmpty());

        for (int i = 0; i < buffer.capacity(); i++) {
            assertEquals(i, buffer.take());
        }
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
    }

    @Test
    @DisplayName("Test multithreaded puts and takes")
    void testMultiThreadedPutsAndTakes() throws InterruptedException {
        final int putMax = 10;
        final int takeMax = 7;
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < putMax; i++) {
                try {
                    buffer.put(i);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < takeMax; i++) {
                try {
                    buffer.take();
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        TimeUnit.SECONDS.sleep(2L);

        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());
        assertEquals(putMax - takeMax, buffer.size());

        for (int i = takeMax; i < putMax; i++) {
            assertEquals(i, buffer.take());
        }
    }

}
```

---

### Chapter 02. Using Barriers and Latches

---
