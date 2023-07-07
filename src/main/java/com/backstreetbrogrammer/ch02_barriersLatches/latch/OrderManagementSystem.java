package com.backstreetbrogrammer.ch02_barriersLatches.latch;

import java.util.concurrent.CountDownLatch;

public class OrderManagementSystem {

    public static void main(final String[] args) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);

        final Service authenticationService = new Service("AuthenticationService", 1000, latch);
        final Service dataService = new Service("DataService", 2000, latch);
        final Service orderService = new Service("OrderService", 3000, latch);

        authenticationService.start();
        dataService.start();
        orderService.start();

        latch.await();

        System.out.printf("%nAll the services are initialized now... %nLets start the OMS in main thread [%s]%n",
                          Thread.currentThread().getName());

    }

}
