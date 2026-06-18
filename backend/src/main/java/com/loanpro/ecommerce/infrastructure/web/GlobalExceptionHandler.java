package com.loanpro.ecommerce.infrastructure.web;

import com.loanpro.ecommerce.application.exception.EmptyOrderException;
import com.loanpro.ecommerce.application.exception.InsufficientStockException;
import com.loanpro.ecommerce.application.exception.InvalidCsvException;
import com.loanpro.ecommerce.application.exception.PaymentFailedException;
import com.loanpro.ecommerce.application.exception.PaymentServiceUnavailableException;
import com.loanpro.ecommerce.application.exception.ResourceNotFoundException;
import com.loanpro.ecommerce.application.exception.DuplicateSkuException;
import com.loanpro.ecommerce.application.exception.UnsupportedPaymentMethodException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 404);
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateSku(DuplicateSkuException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCsvException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCsv(InvalidCsvException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 413);
        body.put("error", "Payload Too Large");
        body.put("message", "File size exceeds maximum allowed size of 10MB");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Insufficient Stock");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(EmptyOrderException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyOrder(EmptyOrderException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentFailed(PaymentFailedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Payment Failed");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(402).body(body);
    }

    @ExceptionHandler(PaymentServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentUnavailable(PaymentServiceUnavailableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Service Unavailable");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(UnsupportedPaymentMethodException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedPayment(UnsupportedPaymentMethodException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}