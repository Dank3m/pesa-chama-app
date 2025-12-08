package com.tablebanking.loanmanagement.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private HttpStatus status = HttpStatus.BAD_REQUEST;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
