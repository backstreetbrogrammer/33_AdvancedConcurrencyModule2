package com.backstreetbrogrammer.ch02_barriersLatches.barriers;

import org.junit.jupiter.api.Test;

import static com.backstreetbrogrammer.ch02_barriersLatches.barriers.FindPrimeNumbers.*;

public class FindPrimeNumbersTest {

    @Test
    void testPrimeNumbersBruteForce() {
        System.out.println(primeNumbersBruteForce(30));
    }

    @Test
    void testPrimeNumbersUsingStreams() {
        System.out.println(primeNumbersUsingStreams(30));
    }

    @Test
    void testPrimeNumbersUsingSieveOfEratosthenes() {
        System.out.println(sieveOfEratosthenes(30));
    }
}
