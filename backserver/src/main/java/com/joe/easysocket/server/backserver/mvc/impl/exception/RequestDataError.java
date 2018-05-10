package com.joe.easysocket.server.backserver.mvc.impl.exception;

import com.joe.easysocket.server.common.exception.SystemException;

/**
 * 请求数据异常
 *
 * @author joe
 * @version 2018.05.10 17:02
 */
public class RequestDataError extends SystemException {
    public RequestDataError(String msg) {
        super(msg);
    }
}
