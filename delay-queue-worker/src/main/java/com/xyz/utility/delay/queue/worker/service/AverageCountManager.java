package com.xyz.utility.delay.queue.worker.service;

import java.util.ArrayDeque;
import java.util.Queue;

public class AverageCountManager {

    private final Queue<Double> queue = new ArrayDeque<>();
    private final int size;
    private double average;

    public AverageCountManager(int size) {
        if (size < 2)
            throw new IllegalArgumentException("size cannot be less than 2");
        this.size = size;
    }

    public synchronized void feed(double val) {
        double out = 0D;
        int initialSize = queue.size();
        if (size <= queue.size())
            out = queue.poll();
        queue.add(val);
        average = (average * initialSize + val - out) / queue.size();
        average = Math.round(average * 100.0) / 100.0;
    }

    public double get() {
        return average;
    }

}
