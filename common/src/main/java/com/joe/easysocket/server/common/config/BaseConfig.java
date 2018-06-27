package com.joe.easysocket.server.common.config;

import java.util.Properties;

/**
 * @author joe
 * @version 2018.06.27 15:33
 */
public interface BaseConfig {
    /**
     * 从配置中获取分布式节点配置
     *
     * @return 分布式节点配置
     */
    ClusterConfig getClusterConfig();

    /**
     * 获取用户配置的环境
     *
     * @return 环境信息
     */
    Properties getEnvironment();
}
