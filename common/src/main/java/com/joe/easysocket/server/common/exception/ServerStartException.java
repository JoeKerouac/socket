package com.joe.easysocket.server.common.exception;

/**
 * 服务器启动异常
 *
 * @author joe
 */
public class ServerStartException extends SystemException {
    private static final long serialVersionUID = 6750113110328704622L;

    public ServerStartException(Throwable cause) {
        super(cause);
    }
}
