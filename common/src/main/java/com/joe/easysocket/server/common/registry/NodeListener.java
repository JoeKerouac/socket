package com.joe.easysocket.server.common.registry;

import com.joe.easysocket.server.common.spi.Registry;

/**
 * 节点监听器
 *
 * @author joe
 */
public interface NodeListener {
    /**
     * 节点监听
     *
     * @param registry 事件对应的注册中心
     * @param event    节点事件
     */
    void listen(Registry registry, NodeEvent event);
}
