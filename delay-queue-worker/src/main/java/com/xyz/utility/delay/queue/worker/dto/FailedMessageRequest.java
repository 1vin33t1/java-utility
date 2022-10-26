package com.xyz.utility.delay.queue.worker.dto;

import com.xyz.utility.persistence.model.KafkaMessageCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedMessageRequest {
    private KafkaMessageCache request;
    private String exception;
    private String errorSource;
}
