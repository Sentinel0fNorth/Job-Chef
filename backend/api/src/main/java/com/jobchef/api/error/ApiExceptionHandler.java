package com.jobchef.api.error;

import com.jobchef.shared.api.ApiModels.ErrorResponse;
import com.jobchef.shared.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> apiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status()).body(new ErrorResponse(
                exception.code(), exception.getMessage(), requestId(request)));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ErrorResponse> invalidRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "INVALID_REQUEST", "The request does not match the supported API contract.", requestId(request)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                "INTERNAL_ERROR", "The request could not be completed. Use the requestId when checking server logs.",
                requestId(request)));
    }

    private String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute("requestId");
        if (existing instanceof String value && !value.isBlank()) return value;
        String generated = UUID.randomUUID().toString();
        request.setAttribute("requestId", generated);
        return generated;
    }
}
