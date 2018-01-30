package com.joe.easysocket.server.common.exception;

/**
 * 非法配置
 *
 * @author joe
 */
public class ConfigIllegalException extends SystemException {
    public ConfigIllegalException(String message) {
        super(message);
    }

    public ConfigIllegalException(String message, Throwable cause) {
        super(message, cause);
    }
}
