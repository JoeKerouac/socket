package com.joe.easysocket.client.core;

/**
 * @author joe
 */
public enum SocketEvent {
    RECEIVE("收到消息"), FAILD("连接异常，断开连接"), REGISTER("连接注册成功"), UNREGISTER("连接被客户端主动注销"), RECONNECT("断线重连");
    private String status;

    SocketEvent(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.status;
    }
}
