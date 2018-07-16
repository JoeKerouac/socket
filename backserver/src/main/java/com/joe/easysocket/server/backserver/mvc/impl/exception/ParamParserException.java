package com.joe.easysocket.server.backserver.mvc.impl.exception;

import com.joe.easysocket.server.common.exception.SystemException;

/**
 * 参数解析异常
 *
 * @author joe
 */
public class ParamParserException extends SystemException {
    private static final long serialVersionUID = 8169673786131228822L;

    /**
     * 参数解析异常
     *
     * @param param   异常的参数名
     * @param message 异常原因
     */
    public ParamParserException(String param, String message) {
        super(String.format("参数%s校验异常，异常原因：%s", param, message));
    }
}
