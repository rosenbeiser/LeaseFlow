package com.leaseflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when no billing strategy exists for the requested property/utility combination. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedUtilityException extends RuntimeException {
    public UnsupportedUtilityException(String message) {
        super(message);
    }
}

