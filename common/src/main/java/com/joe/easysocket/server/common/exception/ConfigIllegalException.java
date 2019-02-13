package com.joe.easysocket.server.common.exception;

/**
 * 非法配置
 *
 * @author joe
 */
public class ConfigIllegalException extends SystemException {

    public ConfigIllegalException() {
        super();
    }

    public ConfigIllegalException(String message) {
        super(message);
    }

    public ConfigIllegalException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigIllegalException(Throwable cause) {
        super(cause);
    }

    protected ConfigIllegalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
