package com.joe.easysocket.client.exception;

/**
 * 系统异常
 *
 * @author joe
 */
public class SystemException extends RuntimeException {
    private static final long serialVersionUID = 8644970275108500846L;

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
