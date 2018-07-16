package com.joe.easysocket.server.balance.protocol;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.joe.easysocket.server.balance.spi.EventCenter;

/**
 * 事件中心
 *
 * @author joe
 */
public class DefaultEventCenter implements EventCenter {
    private List<ProtocolEventListener> listeners;

    public DefaultEventCenter() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void register(ProtocolEventListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void discard(String channel, byte[] data) {
        listeners.forEach(listener -> listener.discard(channel, data));
    }

    @Override
    public void close(String channel, CloseCause closeCause) {
        listeners.forEach(listener -> listener.close(channel, closeCause));
    }

    @Override
    public void send(String channel, byte[] data) {
        listeners.forEach(listener -> listener.send(channel, data));
    }

    @Override
    public void register(String channel) {
        listeners.forEach(listener -> listener.register(channel));
    }

    @Override
    public void receiveError(String channel, byte[] data, Throwable e) {
        listeners.forEach(listener -> listener.receiveError(channel, data, e));
    }

    @Override
    public void receiveSuccess(String channel, byte[] data) {
        listeners.forEach(listener -> listener.receiveSuccess(channel, data));
    }

    @Override
    public void receive(String channel, byte[] data) {
        listeners.forEach(listener -> listener.receive(channel, data));
    }
}
