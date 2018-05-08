package com.joe.easysocket.client.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author joe
 * @version 2018.05.08 15:07
 */
public class EventCenter implements EventListener{
    private AtomicInteger counter;
    private Map<Integer, EventListener> listeners;

    public EventCenter() {
        this.counter = new AtomicInteger(0);
        this.listeners = new ConcurrentHashMap<>();
    }

    /**
     * 注册监听器
     * @param listener
     * 监听器
     * @return
     * 监听器ID，用于移除使用
     */
    public int register(EventListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener must not be null");
        }
        int id = counter.getAndAdd(1);
        listeners.put(id, listener);
        return id;
    }

    /**
     * 移除指定ID的监听器
     * @param id
     * 监听器ID
     */
    public void unregister(int id) {
        listeners.remove(id);
    }

    /**
     * 移除所有监听器
     */
    public void unregisterAll() {
        listeners.clear();
    }

    @Override
    public void listen(SocketEvent event, Object... args) {
        listeners.values().forEach(listener -> listener.listen(event , args));
    }
}
