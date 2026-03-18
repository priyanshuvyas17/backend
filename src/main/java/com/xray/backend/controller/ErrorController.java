package com.xray.backend.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Explicit /error mapping so API clients receive JSON instead of Whitelabel HTML.
 */
@RestController
public class ErrorController {

    @GetMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        int code = status != null ? (Integer) status : HttpStatus.INTERNAL_SERVER_ERROR.value();
        String msg = message != null ? message.toString() : "An error occurred";
        return ResponseEntity.status(code)
                .body(Map.of(
                        "status", "error",
                        "message", msg,
                        "errorCode", code == 401 ? "UNAUTHORIZED" : code == 403 ? "FORBIDDEN" : "ERROR"
                ));
    }
}
