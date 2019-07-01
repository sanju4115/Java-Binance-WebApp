package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class CustomExceptions {

    private static final long serialVersionUID = -1451545022162714730L;

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class FileDoesNotExistException extends RuntimeException {

        public FileDoesNotExistException(String exception) {
            super(exception);
        }
    }

}
