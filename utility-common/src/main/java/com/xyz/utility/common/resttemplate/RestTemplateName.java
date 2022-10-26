package com.xyz.utility.common.resttemplate;

public enum RestTemplateName {
    DELAY_KAFKA_FAILURE("delay.kafka.failure");

    String prefix;

    RestTemplateName(String prefix) {
        this.prefix = prefix;
    }
}
