package com.joe.easysocket.server.backserver.mvc.impl.exception;


import com.joe.easysocket.server.common.exception.SystemException;

/**
 * 不支持的mediatype异常
 *
 * @author joe
 */
public class MediaTypeNoSupportException extends SystemException {
    private static final long serialVersionUID = -4053758179057526205L;

    public MediaTypeNoSupportException(String mediaType) {
        super("不支持" + mediaType);
    }
}
