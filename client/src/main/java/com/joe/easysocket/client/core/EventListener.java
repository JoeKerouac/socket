package com.joe.easysocket.client.core;

import com.joe.easysocket.client.ext.EventListenerAdapter;

/**
 * 事件监听器，推荐使用事件适配器{@link EventListenerAdapter}
 *
 * @author joe
 */
public interface EventListener {
    /**
     * 时间监听
     *
     * @param event 事件源
     * @param args  事件附加的参数，根据具体参数而定
     */
    void listen(SocketEvent event, Object... args);
}
