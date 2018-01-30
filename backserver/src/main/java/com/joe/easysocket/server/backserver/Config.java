package com.joe.easysocket.server.backserver;

import com.joe.easysocket.server.backserver.mvc.DataWorker;
import com.joe.easysocket.server.backserver.mvc.impl.BeanContainer;
import com.joe.easysocket.server.common.config.ClusterConfig;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * @author joe
 */
@Data
@Builder
public class Config {
    /**
     * 分布式节点配置
     */
    @NonNull
    private ClusterConfig clusterConfig;
    /**
     * 本机IP（公网可以访问到的IP或者域名，客户端查询可用前端时将使用该host）
     */
    @NonNull
    private String host;
    /**
     * 后端名，最好是全局唯一的
     */
    @NonNull
    private String name;
    /**
     * 数据处理器，用于处理前端传来的数据，不传默认使用MVCDataWorker
     */
    private DataWorker dataWorker;
    /**
     * bean容器，为空时采用默认的bean容器实现，生产环境建议使用spring实现该容器而不是使用默认的
     */
    private BeanContainer beanContainer;
    /**
     * 处理数据的线程的最大数量
     */
    @Builder.Default
    private int maxThreadCount = 100;
    /**
     * 处理数据的线程的最小数量
     */
    @Builder.Default
    private int minThreadCount = 100;
    /**
     * 空闲线程存活时间，单位为秒
     */
    @Builder.Default
    private long keyAliveTime = 30;
    /**
     * 线程名字的格式，必须包含%d
     */
    @NonNull
    @Builder.Default
    private String threadName = "数据处理线程%d";
    /**
     * 接收数据的topic（实际使用时会加上后端的ID），前端会将数据pub到该topic
     */
    @NonNull
    @Builder.Default
    private String dataSubTopic = "/dev/sub/data";
}
