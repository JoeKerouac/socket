package com.joe.easysocket.server.common.exception;

/**
 * 序列化异常
 *
 * @author joe
 */
public class SerializeException extends SystemException {
    public SerializeException() {
        super();
    }

    public SerializeException(String message) {
        super(message);
    }

    public SerializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }
}
