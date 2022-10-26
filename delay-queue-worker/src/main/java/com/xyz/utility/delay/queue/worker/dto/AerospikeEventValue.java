package com.xyz.utility.delay.queue.worker.dto;

import lombok.Data;

@Data
public class AerospikeEventValue {

    @Data
    public static class Metadata {
        private String namespace;
        private String set;
        private String userKey;
        private String digest;
        private String msg;
    }

    private Metadata metadata;
}
