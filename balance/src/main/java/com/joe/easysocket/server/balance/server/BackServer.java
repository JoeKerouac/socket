package com.joe.easysocket.server.balance.server;

import com.joe.easysocket.server.common.info.BackServerInfo;
import com.joe.easysocket.server.common.lambda.Endpoint;
import com.joe.easysocket.server.common.msg.DataMsg;

public interface BackServer extends Endpoint {
    /**
     * 往真实后端发消息
     *
     * @param msg 要发送的消息
     */
    void write(DataMsg msg);

    /**
     * 从后端读取响应（当发送到后端的消息处理完毕后，后端会把处理结果响应回来，当前端收到消息后会调用该方法）
     *
     * @param msg 响应的消息
     */
    void read(DataMsg msg);

    /**
     * 更新后端信息
     *
     * @param serverInfo 后端信息
     * @return 更新后的后端信息
     */
    BackServer update(BackServerInfo serverInfo);

    /**
     * 获取虚拟服务端的名称
     *
     * @return 虚拟服务端名称
     */
    String getName();

    /**
     * 获取虚拟服务端的ID
     *
     * @return 虚拟服务端的ID
     */
    String getId();

    /**
     * 获取后端信息
     *
     * @return 后端信息
     */
    BackServerInfo getServerInfo();

    //    /**
    //     * 获取当前虚拟服务端队列中等待发送的数据队列的长度
    //     *
    //     * @return 当前虚拟服务端等待发送队列的长度
    //     */
    //    long getWait();
}
