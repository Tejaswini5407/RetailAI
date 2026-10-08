package com.retailai.exception;

public class MLServiceUnavailableException extends RuntimeException {
    public MLServiceUnavailableException(String message) {
        super(message);
    }
}
