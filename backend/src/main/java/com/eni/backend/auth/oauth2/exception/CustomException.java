package com.eni.backend.auth.oauth2.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;

}