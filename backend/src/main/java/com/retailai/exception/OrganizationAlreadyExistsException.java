package com.retailai.exception;

public class OrganizationAlreadyExistsException extends RuntimeException {
    public OrganizationAlreadyExistsException(String message) {
        super(message);
    }
}
