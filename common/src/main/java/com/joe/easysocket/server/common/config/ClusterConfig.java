package com.joe.easysocket.server.common.config;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;

/**
 * 分布式节点配置（更改注册地址时一般只需要修改组地址balanceGroup和backServerGroup即可，这将会使组件处于不同组内而
 * 不会互相发现）
 *
 * @author joe
 */
@Builder
@Getter
public class ClusterConfig {
    /**
     * pub/sub中心类名，需要保证一个topic所有端都能收到（包括前端、后端），优先采用环境中的，如果环境中没有实现那么采用该配置
     */
    private String publishCenter;
    /**
     * 注册中心类名，优先采用环境中的，如果环境中没有实现那么采用该配置
     */
    private String registry;
    /**
     * 序列化器，可以为null，序列化消息使用，当用户没有传入时使用默认的JSON序列化器
     */
    @Singular
    private List<String> serializers;
    /**
     * 消息ACK topic组名（系统会自动在后边添加上前端的ID保证topic唯一，所以不用担心其他前端收到该前端的消息）
     */
    @NonNull
    @Builder.Default
    private String msgAck = "/dev/msg/ack";
    /**
     * 消息响应topic组名（系统会自动在后边添加上前端的ID保证topic唯一，所以不用担心其他前端收到该前端的消息）
     */
    @NonNull
    @Builder.Default
    private String msgResp = "/dev/msg/resp";
    /**
     * 注册中心根路径，以/开头，不能以/结尾，例如可以是/app/socket/registry，前端与后端的配置应该一致
     */
    @NonNull
    @Builder.Default
    private String registryBase = "/app/socket/registry";
    /**
     * 前端注册组名（需要与对应的后端配置一致）(registryBase+balanceGroup构成前端注册地址)
     */
    @NonNull
    @Builder.Default
    private String balanceGroup = "/dev/balance";
    /**
     * 后端注册组名（需要与对应的后端配置一致）
     */
    @NonNull
    @Builder.Default
    private String backServerGroup = "/dev/backserver";
    /**
     * 前端接收后端主动发来的数据的topic（系统会自动在后边添加上前端的ID保证topic唯一，所以不用担心其他前端收到该前端的消息）
     */
    @Builder.Default
    private String topic = "/msg/receive";
    /**
     * 后端接收到channel注销消息后的acktopic（需要同一组所有后端一致）
     */
    @NonNull
    @Builder.Default
    private String channelChangeAckTopic = "/channel/change/ack";
    /**
     * channel改变的topic（需要同一组所有后端一致）
     */
    @NonNull
    @Builder.Default
    private String channelChangeTopic = "/channel/change";
}
