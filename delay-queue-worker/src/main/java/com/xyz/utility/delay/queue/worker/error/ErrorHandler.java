package com.xyz.utility.delay.queue.worker.error;

import com.xyz.utility.common.schema.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@ControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({AssertionError.class})
    public ResponseEntity<ApiResponse<Object>> handleAssertionError(AssertionError assertionError) {
        ErrorCode errorCode = ErrorCode.errorCode(assertionError.getMessage());
        final ApiResponse<Object> apiResponse = new ApiResponse<>(errorCode.getHttpStatus().name(), errorCode.getErrorCode(),
                errorCode.getErrorMessage());
        return new ResponseEntity<>(apiResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException illegalArgumentException) {
        ErrorCode errorCode = ErrorCode.errorCode(illegalArgumentException.getMessage());
        final ApiResponse<Object> apiResponse = new ApiResponse<>(errorCode.getHttpStatus().name(), errorCode.getErrorCode(),
                errorCode.getErrorMessage());
        return new ResponseEntity<>(apiResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler({InternalError.class, NullPointerException.class})
    public ResponseEntity<ApiResponse<Object>> handleCommonError(Throwable throwable) {
        ErrorCode errorCode = ErrorCode.errorCode(throwable.getMessage());
        final ApiResponse<Object> apiResponse = new ApiResponse<>(errorCode.getHttpStatus().name(), errorCode.getErrorCode(),
                errorCode.getErrorMessage());
        return new ResponseEntity<>(apiResponse, errorCode.getHttpStatus());
    }

    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
                                                             HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("Web-request: {}, Default exception handling: ", request, ex);
        final ApiResponse<Object> apiResponse = solveKnown(ex, status);
        return new ResponseEntity<>(apiResponse, status);
    }

    private ApiResponse<Object> solveKnown(Exception ex, HttpStatus status) {
        ErrorCode errorCode = ErrorCode.errorCode(ex.getMessage());
        String errorMessage = ex.getMessage();
        final Throwable rootCause = NestedExceptionUtils.getRootCause(ex);
        if (Objects.nonNull(rootCause))
            errorMessage = rootCause.getMessage();
        return new ApiResponse<>(status.name(), errorCode.getErrorCode(), errorMessage);
    }

}