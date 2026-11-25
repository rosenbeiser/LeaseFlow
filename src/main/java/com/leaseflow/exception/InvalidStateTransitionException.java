package com.leaseflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a state transition is attempted that is not valid for the current lease state. */
@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}

