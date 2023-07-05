package com.backstreetbrogrammer.ch02_barriersLatches.barriers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FindPrimeNumbers {

    // O(n^2)
    public static List<Integer> primeNumbersBruteForce(final int n) {
        final List<Integer> primeNumbers = new LinkedList<>();
        if (n >= 2) {
            primeNumbers.add(2);
        }
        for (int i = 3; i <= n; i += 2) {
            if (isPrimeBruteForce(i)) {
                primeNumbers.add(i);
            }
        }
        return primeNumbers;
    }

    private static boolean isPrimeBruteForce(final int number) {
        for (int i = 2; i * i <= number; i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    // O(n^2)
    public static List<Integer> primeNumbersUsingStreams(final int n) {
        return IntStream.rangeClosed(2, n)
                        .filter(FindPrimeNumbers::isPrime).boxed()
                        .collect(Collectors.toList());
    }

    private static boolean isPrime(final int number) {
        return IntStream.rangeClosed(2, (int) (Math.sqrt(number)))
                        .allMatch(n -> number % n != 0);
    }

    // O(n*log n)
    public static List<Integer> sieveOfEratosthenes(final int n) {
        final boolean[] prime = new boolean[n + 1];
        Arrays.fill(prime, true);
        for (int p = 2; p * p <= n; p++) {
            if (prime[p]) {
                for (int i = p * 2; i <= n; i += p) {
                    prime[i] = false;
                }
            }
        }
        final List<Integer> primeNumbers = new LinkedList<>();
        for (int i = 2; i <= n; i++) {
            if (prime[i]) {
                primeNumbers.add(i);
            }
        }
        return primeNumbers;
    }

}
