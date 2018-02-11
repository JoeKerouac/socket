package com.joe.easysocket.server.balance.protocol;

/**
 * Session关闭原因
 *
 * @author joe
 */
public enum CloseCause {
    USER("用户主动关闭"), TIMEOUT("心跳超时"), SYSTEM("系统错误"), APPLICATION("应用层关闭");
    private String data;

    CloseCause(String data) {
        this.data = data;
    }

    public String toString() {
        return this.data;
    }
}
