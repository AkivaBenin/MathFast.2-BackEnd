package com.mathfast.dto;

import com.mathfast.exception.AppErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private Integer errorCode;
    private String message;
    private String devStackTrace;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(AppErrorCode code, String message, String stackTrace) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(code.getCode())
                .message(message)
                .devStackTrace(stackTrace)
                .build();
    }
}
