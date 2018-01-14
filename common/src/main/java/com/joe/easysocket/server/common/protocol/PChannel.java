package com.joe.easysocket.server.common.protocol;

/**
 * @author joe
 */
public interface PChannel {
    /**
     * 往该channel写数据
     *
     * @param data 要写的数据
     * @return 异步结果
     */
    ProtocolFuture write(byte[] data);

    /**
     * 心跳，当接收到数据时应该触发
     */
    void heartbeat();

    /**
     * 获取Channel的ID
     *
     * @return 该Channel的ID
     */
    String id();

    /**
     * 获取远程主机的IP地址
     *
     * @return 远程主机的IP地址
     */
    String getRemoteHost();

    /**
     * 获取远程主机的端口号
     *
     * @return 远程主机的端口号
     */
    int getPort();

    /**
     * 关闭连接，该方法必须保证不会抛出异常
     */
    void close();

    /**
     * 当前连接是否关闭
     *
     * @return 返回true表示当前连接已关闭
     */
    boolean isClosed();

    /**
     * 获取最后一次传输数据的时间戳
     *
     * @return 最后一次传输数据的时间戳
     */
    long getLastActive();
}
