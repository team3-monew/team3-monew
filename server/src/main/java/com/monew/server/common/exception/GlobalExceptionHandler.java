package com.monew.server.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        log.warn("Business exception. code={}, message={}, details={}",
                e.getErrorCode().name(), e.getMessage(), e.getDetails());

        ErrorResponse response = new ErrorResponse(
                e,
                e.getErrorCode().getStatus().value()
        );

        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();

        e.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                details.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                details.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "VALIDATION_ERROR",
                "요청 데이터 유효성 검사에 실패하였습니다.",
                details,
                e.getClass().getSimpleName(),
                400
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, Object> details = new LinkedHashMap<>();

        e.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".")
                    ? path.substring(path.lastIndexOf('.') + 1)
                    : path;

            details.put(field, violation.getMessage());
        });

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "VALIDATION_ERROR",
                "요청 데이터 유효성 검사에 실패하였습니다.",
                details,
                e.getClass().getSimpleName(),
                400
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", e.getParameterName());
        details.put("parameterType", e.getParameterType());

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "INVALID_REQUEST",
                "잘못된 요청입니다.",
                details,
                e.getClass().getSimpleName(),
                400
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("header", e.getHeaderName());

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "INVALID_REQUEST",
                "잘못된 요청입니다.",
                details,
                e.getClass().getSimpleName(),
                400
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", e.getName());
        details.put("value", e.getValue());

        if (e.getRequiredType() != null) {
            details.put("requiredType", e.getRequiredType().getSimpleName());
        }

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "INVALID_REQUEST",
                "잘못된 요청입니다.",
                details,
                e.getClass().getSimpleName(),
                400
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected server exception", e);

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다.",
                new LinkedHashMap<>(),
                e.getClass().getSimpleName(),
                500
        );

        return ResponseEntity.internalServerError().body(response);
    }
}