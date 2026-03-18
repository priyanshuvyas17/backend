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
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageObj = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        int code = (statusObj instanceof Integer) ? (Integer) statusObj : HttpStatus.INTERNAL_SERVER_ERROR.value();
        String msg = (messageObj != null) ? messageObj.toString() : "An error occurred";
        return ResponseEntity.status(code)
                .body(Map.of(
                        "status", "error",
                        "message", msg,
                        "errorCode", code == 401 ? "UNAUTHORIZED" : code == 403 ? "FORBIDDEN" : "ERROR"
                ));
    }
}
