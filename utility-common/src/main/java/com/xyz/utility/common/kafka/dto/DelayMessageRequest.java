package com.xyz.utility.common.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelayMessageRequest {
    private String topic;
    private String key;
    private Object value;
    private Map<String, String> headers;
    private long postTime;
    private String failurePostCallbackUrl;
}