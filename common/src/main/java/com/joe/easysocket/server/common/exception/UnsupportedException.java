package com.joe.easysocket.server.common.exception;

/**
 * 不支持异常
 *
 * @author joe
 * @version 2018.06.21 18:40
 */
public class UnsupportedException extends SystemException {
    public UnsupportedException() {
        super();
    }

    public UnsupportedException(String message) {
        super(message);
    }

    public UnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
