package com.xyz.utility.common.schema;

import com.xyz.utility.common.constant.UtilityConstant;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.MDC;

import java.io.Serializable;
import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode(exclude = {"timestamp", "requestId"})
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 5216105825101881417L;

    private String requestId;
    private String errorCode;
    private String errorMessage;
    private String status;
    private Instant timestamp;
    private T data;

    private ApiResponse() {
        requestId = MDC.get(UtilityConstant.TRACE_ID);
        timestamp = Instant.now();
    }

    public ApiResponse(T data) {
        this();
        this.data = data;
    }

    public ApiResponse(String status, T data) {
        this(data);
        this.status = status;
    }

    public ApiResponse(String status, String errorCode, String errorMessage) {
        this();
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}