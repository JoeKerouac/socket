package com.joe.easysocket.server.common.config;

/**
 * 常量表（主要是环境中的key）
 *
 * @author joe
 * @version 2018.06.27 13:39
 */
public final class Const {

    /**
     * 配置信息
     */
    public static String       CONFIG          = "config";

    public static String       CLUSTER_CONFIG  = "clusterConfig";

    /**
     * 注册中心
     */
    public static String       REGISTRY        = "registry";

    /**
     * PUB/SUB消息中心
     */
    public static String       PUBLISH_CENTER  = "publishCenter";

    /**
     * Redis配置
     */
    public static String       REDIS_CONFIG    = "redisConfig";

    /**
     * zookeeper配置
     */
    public static String       ZK_CONFIG       = "zkConfig";

    /**
     * 序列化器LIST
     */
    public static String       SERIALIZER_LIST = "serializers";

    /**
     * 协议栈组件
     */
    public static final String PROTOCOL        = "protocol";

    /**
     * 路由器组件
     */
    public static final String ROUTER          = "router";
}
