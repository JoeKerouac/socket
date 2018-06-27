package com.joe.easysocket.server.balance;

import com.joe.easysocket.server.balance.spi.EventCenter;
import com.joe.easysocket.server.common.lambda.Function;

/**
 * 前端
 *
 * @author joe
 * @version 2018.06.26 17:30
 */
public interface Balance extends EventCenter{
    /**
     * 启动前端
     *
     * @param callback 前端关闭时的回调
     */
    void start(Function callback);

    /**
     * 关闭前端
     */
    void shutdown();
}
