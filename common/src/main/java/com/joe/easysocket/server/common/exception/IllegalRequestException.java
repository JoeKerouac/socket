package com.joe.easysocket.server.common.exception;

/**
 * 非法请求，当请求数据不符合要求时会抛出该异常
 *
 * @author joe
 */
public class IllegalRequestException extends SystemException {
    private static final long serialVersionUID = 855898103206510828L;

    /**
     * 非法请求
     *
     * @param cause 非法请求来源
     */
    public IllegalRequestException(String cause) {
        super("非法请求：" + cause);
    }

    /**
     * 非法请求
     *
     * @param cause 非法请求导致的异常
     */
    public IllegalRequestException(Throwable cause) {
        super("非法请求：", cause);
    }
}
