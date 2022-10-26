package com.xyz.utility.delay.queue.worker.error;


import com.xyz.utility.common.constant.UtilityConstant;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;


@Getter
public enum ErrorCode {

    SAVE_TO_DELAY_DB_VALIDATION_FAILED("UTIL-4001", UtilityConstant.SAVE_TO_DELAY_DB_VALIDATION_FAILED, HttpStatus.BAD_REQUEST),
    HASH_VALIDATION_FAILED("UTIL-4002", UtilityConstant.HASH_VALIDATION_FAILED, HttpStatus.BAD_REQUEST),
    UNCAUGHT_EXCEPTION("UTIL-5000", "Due to some technical issue not able to respond", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private static final Map<String, ErrorCode> errorCodeMessageMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        EnumSet.allOf(ErrorCode.class).forEach(code -> errorCodeMessageMap.put(code.errorMessage, code));
    }

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String errorCode, String errorMessage, HttpStatus httpStatus) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
    }

    public static ErrorCode errorCode(String msg) {
        return errorCodeMessageMap.getOrDefault(msg, UNCAUGHT_EXCEPTION);
    }
}