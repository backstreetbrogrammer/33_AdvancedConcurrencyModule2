package com.backstreetbrogrammer.ch01_locksSemaphores;

public class Student {

    public final Object lock = new Object();

    public void admit() {
        synchronized (lock) {
            // code for admission
        }
    }

}
