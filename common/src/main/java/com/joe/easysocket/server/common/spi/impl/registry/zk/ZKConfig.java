package com.joe.easysocket.server.common.spi.impl.registry.zk;

import com.joe.easysocket.server.common.lambda.Serializer;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;

/**
 * zk config
 *
 * @author joe
 */
@Data
@Builder
public class ZKConfig {
    /**
     * zookeeper连接，例如：localhost:8080,ab.com:90
     */
    @NonNull
    private String connectStr;
    /**
     * max number of times to retry
     */
    @Builder.Default
    private int maxRetry = 5;
    /**
     * initial amount of time to wait between retries
     */
    @Builder.Default
    private int baseSleepTimeMs = 1000;
    /**
     * serializer array，不配置则使用默认的JSON序列化
     */
    @Singular
    private List<Serializer> serializers;
}
