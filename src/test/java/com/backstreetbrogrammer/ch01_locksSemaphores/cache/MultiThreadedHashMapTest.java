package com.backstreetbrogrammer.ch01_locksSemaphores.cache;

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
