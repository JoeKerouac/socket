package com.joe.easysocket.server.common.lambda;


import com.joe.easysocket.server.common.exception.SystemException;

/**
 * 启动、关闭接口
 *
 * @author joe
 */
public interface Endpoint {
    /**
     * 启动
     *
     * @throws SystemException 启动过程中有可能发生异常
     */
    void start() throws SystemException;

    /**
     * 关闭
     *
     * @throws SystemException 关闭过程中有可能发生异常
     */
    void shutdown() throws SystemException;
}
