package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.kafka.dto.DelayMessageRequest;

public interface SupportService {
    void saveToDelayDb(String messageId, String key, DelayMessageRequest request);
    void saveToDelayDb(int count);

}
