package com.copick.groups.exception;

import com.copick.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidGroupNameException extends DomainException {
    public InvalidGroupNameException(String reason) {
        super(reason, HttpStatus.BAD_REQUEST, "INVALID_GROUP_NAME");
    }
}
