package com.pahadcarry.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PahadCarryException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus status;

    public PahadCarryException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public static PahadCarryException badRequest(ErrorCode errorCode, String message) {
        return new PahadCarryException(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public static PahadCarryException notFound(ErrorCode errorCode, String message) {
        return new PahadCarryException(errorCode, message, HttpStatus.NOT_FOUND);
    }

    public static PahadCarryException forbidden(ErrorCode errorCode, String message) {
        return new PahadCarryException(errorCode, message, HttpStatus.FORBIDDEN);
    }

    public static PahadCarryException conflict(ErrorCode errorCode, String message) {
        return new PahadCarryException(errorCode, message, HttpStatus.CONFLICT);
    }
}
