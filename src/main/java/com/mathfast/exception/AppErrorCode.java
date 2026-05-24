package com.mathfast.exception;

import lombok.Getter;

@Getter
public enum AppErrorCode {
    ROOM_CLOSED(4001),
    EMPTY_ROOM(4002),
    TOKEN_EXPIRED(4003),
    EXPLOIT_DETECTED(4004),
    INTERNAL_SERVER_ERROR(5001);

    private final int code;

    AppErrorCode(int code) {
        this.code = code;
    }
}
