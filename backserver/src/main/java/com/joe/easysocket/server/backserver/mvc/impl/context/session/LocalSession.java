package com.joe.easysocket.server.backserver.mvc.impl.context.session;


import com.joe.easysocket.server.backserver.mvc.context.Session;
import com.joe.easysocket.server.common.protocol.ChannelProxy;

import java.util.Map;
import java.util.TreeMap;

/**
 * 本地session，服务器关闭session内容就会丢失
 *
 * @author joe
 */
public class LocalSession implements Session {
    // 会话缓存
    private Map<String, Object> cache;
    private int port;
    private String host;
    private String channel;
    private ChannelProxy pChannel;

    public LocalSession(String channel, String host, int port, ChannelProxy pChannel) {
        this.cache = new TreeMap<>();
        this.channel = channel;
        this.host = host;
        this.port = port;
        this.pChannel = pChannel;
    }

    @Override
    public String getRemoteHost() {
        return this.host;
    }

    @Override
    public int getRemotePort() {
        return this.port;
    }

    @Override
    public String getId() {
        return this.channel;
    }

    @Override
    public void setAttribute(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return cache.get(key);
    }

    @Override
    public ChannelProxy getChannel() {
        return this.pChannel;
    }
}
