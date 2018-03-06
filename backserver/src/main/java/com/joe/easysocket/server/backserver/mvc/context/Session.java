package com.joe.easysocket.server.backserver.mvc.context;

import com.joe.easysocket.server.common.protocol.ChannelProxy;

/**
 * Session
 *
 * @author joe
 */
public interface Session {
    /**
     * 获取远程地址，格式如：192.168.1.1
     *
     * @return 远程地址
     */
    String getRemoteHost();

    /**
     * 获取远程端口
     *
     * @return 远程端口
     */
    int getRemotePort();

    /**
     * 获取session的ID，该ID应该与session对应的通道ID一致
     *
     * @return
     */
    String getId();

    /**
     * 往session存放数据
     *
     * @param key   key
     * @param value value
     */
    void setAttribute(String key, Object value);

    /**
     * 从session中取数据
     *
     * @param key key
     * @return 指定key对应的数据
     */
    Object getAttribute(String key);

    /**
     * 获取对应的通道
     *
     * @return 对应的通道
     */
    ChannelProxy getChannel();
}
