package com.joe.easysocket.server.balance;

import com.joe.easysocket.server.balance.protocol.DefaultEventCenter;
import com.joe.easysocket.server.balance.protocol.netty.NettyConnectorManager;
import com.joe.easysocket.server.balance.strategy.LoadStrategy;
import com.joe.easysocket.server.balance.strategy.PollLoadSrategy;
import com.joe.easysocket.server.common.config.ClusterConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * 前端配置，其中发布中心、注册中心、本机IP为必填项，其他都为选填项
 *
 * @author joe
 */
@Getter
@Builder
public class Config {
    @NonNull
    private ClusterConfig clusterConfig;
    /**
     * 本机IP（公网可以访问到的IP或者域名，客户端查询可用前端时将使用该host）
     */
    @NonNull
    private String host;
    /**
     * 负载策略
     */
    @NonNull
    @Builder.Default
    private LoadStrategy strategy = new PollLoadSrategy();
    /**
     * 监听客户端的端口
     */
    @Builder.Default
    private int port = 10051;
    /**
     * TCP连接的backlog
     */
    @Builder.Default
    private int tcpBacklog = 1024;
    /**
     * 心跳周期（与客户端的连接），单位为秒，不能小于30秒
     */
    @Builder.Default
    private int heatbeat = 300;
    @Builder.Default
    private boolean nodelay = true;
    /**
     * 响应超时时间，单位毫秒
     */
    @Builder.Default
    private long respTimeout = 5000;
    /**
     * ack超时时间，单位毫秒
     */
    @Builder.Default
    private long ackTimeout = 1000;
    /**
     * 连接管理
     */
    @NonNull
    @Builder.Default
    private String connectorManager = NettyConnectorManager.class.getName();
    /**
     * 事件中心
     */
    @NonNull
    @Builder.Default
    private String eventCenter = DefaultEventCenter.class.getName();
}
