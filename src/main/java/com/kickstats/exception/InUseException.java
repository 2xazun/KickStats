package com.kickstats.exception;

public class InUseException extends RuntimeException {

    public InUseException(String message) {
        super(message);
    }
}
