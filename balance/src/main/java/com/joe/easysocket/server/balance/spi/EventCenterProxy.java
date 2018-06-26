package com.joe.easysocket.server.balance.spi;

import com.joe.easysocket.server.balance.protocol.CloseCause;
import com.joe.easysocket.server.balance.protocol.ProtocolEventListener;

/**
 * 对EventCenter进行代理，必须先调用{@link #setEventCenter(EventCenter) setEventCenter}方法才能使用
 *
 * @author joe
 * @version 2018.06.26 17:38
 */
public class EventCenterProxy implements EventCenter {
    private EventCenter proxy;

    /**
     * 设置代理的EventCenter
     *
     * @param proxy 代理的EventCenter
     */
    public void setEventCenter(EventCenter proxy) {
        this.proxy = proxy;
    }

    @Override
    public void register(ProtocolEventListener listener) {
        proxy.register(listener);
    }

    @Override
    public void discard(String channel, byte[] data) {
        proxy.discard(channel, data);
    }

    @Override
    public void close(String channel, CloseCause closeCause) {
        proxy.close(channel, closeCause);
    }

    @Override
    public void send(String channel, byte[] data) {
        proxy.send(channel, data);
    }

    @Override
    public void register(String channel) {
        proxy.register(channel);
    }

    @Override
    public void receiveError(String channel, byte[] data, Throwable e) {
        proxy.receiveError(channel, data, e);
    }

    @Override
    public void receiveSuccess(String channel, byte[] data) {
        proxy.receiveSuccess(channel, data);
    }

    @Override
    public void receive(String channel, byte[] data) {
        proxy.receive(channel, data);
    }
}
