package com.joe.easysocket.server.balance;

import java.util.Properties;

import com.joe.easysocket.server.balance.protocol.DefaultEventCenter;
import com.joe.easysocket.server.balance.protocol.netty.tcp.TCPConnectorManager;
import com.joe.easysocket.server.balance.strategy.LoadStrategy;
import com.joe.easysocket.server.balance.strategy.PollLoadSrategy;
import com.joe.easysocket.server.common.config.BaseConfig;
import com.joe.easysocket.server.common.config.ClusterConfig;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * 前端配置，其中发布中心、注册中心、本机IP为必填项，其他都为选填项
 *
 * @author JoeKerouac
 */
@Getter
@Builder
public class Config implements BaseConfig {

    /**
     * 分布式节点配置
     */
    @NonNull
    private ClusterConfig clusterConfig;

    /**
     * 本机IP（公网可以访问到的IP或者域名，客户端查询可用前端时将使用该host）
     */
    @NotEmpty
    private String        host;

    /**
     * 负载策略
     */
    @NonNull
    @Builder.Default
    private LoadStrategy  strategy         = new PollLoadSrategy();

    /**
     * 监听客户端的端口
     */
    @Size(min = 1, max = 65536)
    @Builder.Default
    private int           port             = 10051;

    /**
     * TCP连接的backlog
     */
    @Min(value = 1)
    @Builder.Default
    private int           tcpBacklog       = 1024;

    /**
     * 心跳周期（与客户端的连接），单位为秒，不能小于30秒
     */
    @Min(value = 30)
    @Builder.Default
    private int           heatbeat         = 300;

    @Builder.Default
    private boolean       nodelay          = true;

    /**
     * 全局响应超时时间，单位毫秒
     */
    @Min(value = 1)
    @Builder.Default
    private long          respTimeout      = 5000;

    /**
     * ack超时时间，单位毫秒
     */
    @Min(value = 1)
    @Builder.Default
    private long          ackTimeout       = 1000;
    /**
     * 用户环境信息
     */
    @Builder.Default
    private Properties    environment;
    /**
     * 连接管理
     */
    @NotEmpty
    @Builder.Default
    private String        connectorManager = TCPConnectorManager.class.getName();
    /**
     * 事件中心
     */
    @NotEmpty
    @Builder.Default
    private String        eventCenter      = DefaultEventCenter.class.getName();
}
