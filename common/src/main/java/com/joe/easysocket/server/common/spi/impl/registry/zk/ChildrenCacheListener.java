package com.joe.easysocket.server.common.spi.impl.registry.zk;

import com.joe.easysocket.server.common.spi.NodeEvent;

/**
 * 监听节点变化，只监听第一层节点变化，不监听子节点的变化，例如对/test监听，那么/test/a/b节点的
 * 变化事件不会触发该监听
 *
 * @author joe
 */
public interface ChildrenCacheListener {
    /**
     * 当节点变化时触发该方法
     *
     * @param client 客户端代理
     * @param event  事件
     * @throws Exception errors
     */
    void childEvent(ZKClient client, NodeEvent event) throws Exception;
}
