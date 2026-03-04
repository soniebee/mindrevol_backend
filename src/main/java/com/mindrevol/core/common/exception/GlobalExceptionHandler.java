package com.mindrevol.core.common.exception;

import com.mindrevol.core.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Helper method mới
    private <T> ResponseEntity<ApiResponse<T>> buildResponse(HttpStatus status, String message, String errorCode) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), message, errorCode));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        // Log mức INFO hoặc WARN thôi, không cần ERROR cho lỗi 404
        log.warn("Not Found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND");
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad Request: {}", ex.getMessage());
        // Có thể mở rộng BadRequestException để chứa errorCode riêng nếu cần
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "BAD_REQUEST");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(DisabledException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "ACCOUNT_DISABLED");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác.", "BAD_CREDENTIALS");
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation Error: {}", errors);
        
        // Dùng method error có data
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Dữ liệu không hợp lệ", "VALIDATION_FAILED", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalServerError(Exception ex) {
        // Lỗi 500 luôn phải log ERROR kèm StackTrace để debug
        log.error("Internal Error: ", ex);
        
        // QUAN TRỌNG: Không trả về ex.getMessage() nguyên bản cho client để tránh lộ thông tin hệ thống
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", "INTERNAL_SERVER_ERROR");
    }
    
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Định dạng dữ liệu gửi lên không hợp lệ (sai kiểu dữ liệu hoặc thiếu dấu phẩy).", "MALFORMED_JSON");
    }
    
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        log.warn("File size limit exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Kích thước file tải lên vượt quá giới hạn cho phép.", "FILE_TOO_LARGE");
    }
    
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này.", "ACCESS_DENIED");
    }
}