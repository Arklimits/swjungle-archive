package com.example.demo.component.handler;

import com.example.demo.domain.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Map<Class<? extends Exception>, ErrorCode> exceptionMap;

    public GlobalExceptionHandler() {
        Map<Class<? extends Exception>, ErrorCode> tempMap = new HashMap<>();
        this.exceptionMap = Collections.unmodifiableMap(tempMap);

        for (ErrorCode errorCode : ErrorCode.values()) {
            Set<Class<? extends Exception>> exceptions = errorCode.getExceptions();
            for (Class<? extends Exception> exception : exceptions) {
                tempMap.put(exception, errorCode);
            }
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handlerException(Exception e) {
        if (exceptionMap.containsKey(e.getClass())) {
            ErrorCode errorCode = exceptionMap.get(e.getClass());

            ErrorResponse errorResponse = ErrorResponse.builder(e, errorCode.getStatus(), e.getClass().getSimpleName())
                    .typeMessageCode("Error Message")
                    .titleMessageCode(errorCode.getCode())
                    .detailMessageCode(errorCode.getMessage())
                    .build();

            return new ResponseEntity<>(errorResponse, errorCode.getStatus());
        }

        // 예상 못한 에러
        final ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.INTERNAL_SERVER_ERROR, e.getClass().getSimpleName())
                .typeMessageCode("Error Message")
                .titleMessageCode("Internal Server Error")
                .detailMessageCode(e.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
