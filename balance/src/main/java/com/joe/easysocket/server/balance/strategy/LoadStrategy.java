package com.joe.easysocket.server.balance.strategy;

import com.joe.easysocket.server.balance.server.BackServer;
import com.joe.easysocket.server.common.info.BackServerInfo;

/**
 * 负载均衡策略（负载器只负责筛选合适的后端，不负责其他逻辑处理，当没有可用的后端时需要返回一个默认的后端）
 *
 * @author joe
 */
public interface LoadStrategy {
    /**
     * 选择一个合适的后端（需要内置一个虚拟后端，在没有可用后端的时候返回该虚拟后端，所有经由该后端发送的数据均加入队列
     * 等待有后端注册进来后发送）
     *
     * @return 后端
     */
    BackServer next();

    /**
     * 添加一个可用的后端
     *
     * @param server 后端
     */
    void addLoad(BackServer server);

    /**
     * 删除一个后端
     *
     * @param id 要删除的后端的ID
     */
    void removeLoad(String id);

    /**
     * 更新一个后端信息
     *
     * @param id   后端ID
     * @param info 后端信息
     */
    void update(String id, BackServerInfo info);

    /**
     * 清除所有后端
     */
    void clear();
}
