package com.backstreetbrogrammer.ch02_barriersLatches.barriers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CyclicBarrierDemo {

    private static List<Integer> findPrimes(final List<Integer> nList) {
        if (nList == null || nList.isEmpty()) throw new IllegalArgumentException();

        final List<Integer> result = new ArrayList<>();
        for (final int num : nList) {
            if (isPrime(num)) {
                result.add(num);
            }
        }
        return result;
    }

    private static boolean isPrime(final int number) {
        return IntStream.rangeClosed(2, (int) (Math.sqrt(number)))
                        .allMatch(n -> number % n != 0);
    }

    private static List<Integer> generateList(final int low, final int high) {
        return Stream.iterate(low, n -> n + 1)
                     .limit(high)
                     .collect(Collectors.toList());
    }

    public static void main(final String[] args) {

        class PrimeNumbersFinder implements Callable<List<Integer>> {
            private final CyclicBarrier barrier;
            private final List<Integer> inputList;

            public PrimeNumbersFinder(final CyclicBarrier barrier, final List<Integer> inputList) {
                this.barrier = barrier;
                this.inputList = inputList;
            }

            public List<Integer> call() {
                final List<Integer> result = findPrimes(inputList);
                try {
                    System.out.printf("[%s] Just arrived for input list %s, waiting for the others...%n",
                                      Thread.currentThread().getName(), inputList);
                    barrier.await();
                    System.out.printf("[%s] Done waiting, return the result now %s%n",
                                      Thread.currentThread().getName(), result);
                } catch (final InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
                return result;
            }
        }

        final CyclicBarrier barrier = new CyclicBarrier(4, () -> System.out.printf("%nBarrier opening%n%n"));
        final ExecutorService executorService = Executors.newFixedThreadPool(barrier.getParties());

        final List<Future<List<Integer>>> futures = new ArrayList<>();
        try {
            int low = 1;
            for (int i = 1; i <= barrier.getParties(); i++) {
                final PrimeNumbersFinder primeNumbersFinder = new PrimeNumbersFinder(barrier, generateList(low, 10));
                futures.add(executorService.submit(primeNumbersFinder));
                low = (10 * i) + 1;
            }

            final List<Integer> result = new ArrayList<>();
            futures.forEach(
                    future -> {
                        try {
                            result.addAll(future.get(200L, TimeUnit.MILLISECONDS));
                        } catch (final InterruptedException | ExecutionException e) {
                            System.out.println(e.getMessage());
                        } catch (final TimeoutException e) {
                            System.out.println("Timed out");
                            future.cancel(true);
                        }
                    });

            System.out.printf("%nFinal result: %s%n", result);
        } finally {
            executorService.shutdown();
        }
    }

}
