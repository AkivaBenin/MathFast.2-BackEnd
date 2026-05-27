package com.mathfast.exception;

import com.mathfast.dto.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getStackTraceAsString(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    @ExceptionHandler(ExploitDetectedException.class)
    public ResponseEntity<ApiResponse<Void>> handleExploitDetectedException(ExploitDetectedException ex) {
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.EXPLOIT_DETECTED, ex.getMessage(), getStackTraceAsString(ex));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(WrongAnswerException.class)
    public ResponseEntity<ApiResponse<Void>> handleWrongAnswerException(WrongAnswerException ex) {
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.WRONG_ANSWER, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RoomClosedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomClosedException(RoomClosedException ex) {
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.ROOM_CLOSED, ex.getMessage(), getStackTraceAsString(ex));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(EmptyRoomException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmptyRoomException(EmptyRoomException ex) {
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.EMPTY_ROOM, ex.getMessage(), getStackTraceAsString(ex));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.INVALID_CREDENTIALS, ex.getMessage(), getStackTraceAsString(ex));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex) {
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.USERNAME_ALREADY_EXISTS, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(org.springframework.data.redis.RedisConnectionFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisConnectionFailureException(org.springframework.data.redis.RedisConnectionFailureException ex) {
        log.warn("Redis connectivity failure: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
            AppErrorCode.REDIS_UNAVAILABLE,
            "Redis is unavailable. Please start Redis or check Redis configuration.",
            null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(ExpiredJwtException ex) {
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.TOKEN_EXPIRED, "Token has expired", getStackTraceAsString(ex));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex) {
        log.info("SSE connection timed out, completing thread gracefully to prevent pooling limits.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(Exception ex) {
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.INTERNAL_SERVER_ERROR, "Invalid parameter type: " + ex.getMessage(), getStackTraceAsString(ex));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Internal Server Error", ex);
        ApiResponse<Void> response = ApiResponse.error(AppErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred", getStackTraceAsString(ex));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
