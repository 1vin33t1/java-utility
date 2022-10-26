package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.persistence.model.KafkaMessageCache;

public interface DelayFailureHandler {
    void notifyClient(KafkaMessageCache messageCache, String source, Exception ex);
}
