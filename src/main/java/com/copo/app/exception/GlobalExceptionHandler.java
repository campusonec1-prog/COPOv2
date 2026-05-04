package com.copo.app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle 404 - No route matched
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(NoHandlerFoundException ex, Model model) {
        log.warn("404 Not Found: {}", ex.getRequestURL());
        model.addAttribute("error", "Page not found.");
        model.addAttribute("status", 404);
        return "error"; // templates/error.html
    }

    // Handle general runtime exceptions (production-safe: no stack trace exposed)
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        log.error("Runtime error: {}", ex.getMessage(), ex);
        model.addAttribute("error", "An unexpected error occurred. Please try again or contact support.");
        model.addAttribute("status", 500);
        return "error";
    }

    // Catch-all handler
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        model.addAttribute("error", "An unexpected error occurred. Please try again or contact support.");
        model.addAttribute("status", 500);
        return "error";
    }
}
