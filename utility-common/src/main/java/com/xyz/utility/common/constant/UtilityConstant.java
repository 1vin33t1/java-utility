package com.xyz.utility.common.constant;

public class UtilityConstant {

    private UtilityConstant() {
    }

    public static final String AUDIT_KAFKA_QUEUE = "payments_java_utility_service";
    public static final String DELETE_EVENT = "delete";
    public static final String TRACE_ID = "traceId";
    public static final String AUTHORIZATION = "Authorization";
    public static final String HASH_DELIMITER = "|";
    public static final String SAVE_TO_DELAY_DB_VALIDATION_FAILED = "request is invalid for save delay db";
    public static final String HASH_VALIDATION_FAILED = "hash is invalid";

    public static class Kafka {

        private Kafka() {
        }

        public static final String MESSAGE_ID = "kfMessageId";
        public static final String JSON_KAFKA_TEMPLATE = "jsonKafkaTemplate";
        public static final String FAILURE_CALLBACK_URL = "failureCallbackUrl";
        public static final String AEROSPIKE_SAVE_RETRY_EXHAUST = "not able to save to aerospike";
        public static final String KAFKA_SEND_FAILED = "not able to send message to kafka";
    }

}
