package com.joe.easysocket.server.backserver.mvc.impl.exception;


import com.joe.easysocket.server.common.exception.SystemException;

/**
 * 资源调用异常
 *
 * @author joe
 */
public class ResourceInvokeException extends SystemException {
    private static final long serialVersionUID = -8802366810043428339L;

    public ResourceInvokeException(Throwable cause) {
        super(cause);
    }
}
