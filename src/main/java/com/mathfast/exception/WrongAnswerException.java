package com.mathfast.exception;

public class WrongAnswerException extends RuntimeException {
    public WrongAnswerException(String message) {
        super(message);
    }
}
