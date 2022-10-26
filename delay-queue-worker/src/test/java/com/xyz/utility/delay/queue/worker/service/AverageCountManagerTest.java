package com.xyz.utility.delay.queue.worker.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AverageCountManagerTest {

    @Test
    void testAverage() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AverageCountManager(1));
        var averageCountManager = new AverageCountManager(5);
        averageCountManager.feed(1.9);
        Assertions.assertEquals(1.9, averageCountManager.get());
        averageCountManager.feed(3);
        Assertions.assertEquals(2.45, averageCountManager.get());
        averageCountManager.feed(2);
        Assertions.assertEquals(2.3, averageCountManager.get());
        averageCountManager.feed(2.5);
        Assertions.assertEquals(2.35, averageCountManager.get());
        averageCountManager.feed(1.7);
        Assertions.assertEquals(2.22, averageCountManager.get());
        averageCountManager.feed(2.2);
        Assertions.assertEquals(2.28, averageCountManager.get());
        averageCountManager.feed(2.5);
        Assertions.assertEquals(2.18, averageCountManager.get());
    }

}