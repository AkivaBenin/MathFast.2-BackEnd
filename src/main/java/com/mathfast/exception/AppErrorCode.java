package com.mathfast.exception;

import lombok.Getter;

@Getter
public enum AppErrorCode {
    ROOM_CLOSED(4001),
    EMPTY_ROOM(4002),
    TOKEN_EXPIRED(4003),
    EXPLOIT_DETECTED(4004),
    INVALID_CREDENTIALS(4005),
    WRONG_ANSWER(4006),
    USERNAME_ALREADY_EXISTS(4007),
    INTERNAL_SERVER_ERROR(5001),
    REDIS_UNAVAILABLE(5002);

    private final int code;

    AppErrorCode(int code) {
        this.code = code;
    }
}
