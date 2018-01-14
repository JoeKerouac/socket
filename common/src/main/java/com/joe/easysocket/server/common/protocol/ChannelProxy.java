package com.joe.easysocket.server.common.protocol;

/**
 * @author joe
 */
public interface ChannelProxy {
    /**
     * 往该channel写数据
     *
     * @param invoke 要调用的客户端方法
     * @param data   要写的数据
     * @return 异步结果
     */
    ProtocolFuture write(String invoke, String data);

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
     * 当前连接是否关闭
     *
     * @return 返回true表示当前连接已关闭
     */
    boolean isClosed();


    /**
     * 添加关闭回调
     *
     * @param callback 通道关闭回调（当通道关闭的时候会调用该方法）
     */
    void addCloseCallback(ChannelCloseCallback callback);

    /**
     * 通道关闭回调
     */
    interface ChannelCloseCallback {
        /**
         * 通道关闭回调
         *
         * @param id 关闭的通道id
         */
        void close(String id);
    }
}
