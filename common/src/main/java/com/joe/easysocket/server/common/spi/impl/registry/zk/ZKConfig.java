package com.joe.easysocket.server.common.spi.impl.registry.zk;

import java.util.List;

import com.joe.easysocket.server.common.spi.Serializer;

import lombok.Data;

/**
 * zk config
 *
 * @author joe
 */
@Data
public class ZKConfig {
    /**
     * zookeeper连接，例如：localhost:8080,ab.com:90
     */
    private String           connectStr;
    /**
     * max number of times to retry
     */
    private int              maxRetry        = 5;
    /**
     * initial amount of time to wait between retries
     */
    private int              baseSleepTimeMs = 1000;
    /**
     * serializer array，不配置则使用默认的JSON序列化
     */
    private List<Serializer> serializers;
}
