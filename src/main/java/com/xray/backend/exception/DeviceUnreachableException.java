package com.xray.backend.exception;

public class DeviceUnreachableException extends RuntimeException {

    public DeviceUnreachableException(String message) {
        super(message);
    }
}
