package com.joe.easysocket.server.backserver.mvc;


import com.joe.easysocket.server.common.lambda.Endpoint;
import com.joe.easysocket.server.common.msg.DataMsg;

/**
 * 协议栈数据处理器，bean容器和session管理器可以不注册，但是发布中心和队列必须注册；
 * 因为该处理器依赖这两个组件工作
 *
 * @author joe
 */
public interface DataWorker extends Endpoint {
    /**
     * 读取消息，当前端有消息发送过来时会调用该方法
     *
     * @param msg 前端发送过来的消息
     */
    void read(DataMsg msg);
}
