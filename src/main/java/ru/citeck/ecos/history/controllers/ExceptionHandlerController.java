package ru.citeck.ecos.history.controllers;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(Exception.class)
    public Object exceptionHandler(Exception exception) {
        return ExceptionUtils.getStackTrace(exception);
    }
}
