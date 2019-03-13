package com.joe.easysocket.server.backserver.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.joe.easysocket.server.backserver.mvc.data.InterfaceData;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.config.Const;
import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.data.ProtocolData;
import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.info.BalanceInfo;
import com.joe.easysocket.server.common.lambda.Endpoint;
import com.joe.easysocket.server.common.msg.ChannelId;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.msg.DataMsg;
import com.joe.easysocket.server.common.protocol.ChannelProxy;
import com.joe.easysocket.server.common.protocol.ProtocolFuture;
import com.joe.easysocket.server.common.spi.MessageCenter;
import com.joe.easysocket.server.common.spi.Serializer;
import com.joe.utils.common.Tools;
import com.joe.utils.protocol.Datagram;
import com.joe.utils.protocol.DatagramUtil;
import com.joe.utils.serialize.json.JsonParser;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 虚拟channel管理器
 *
 * @author joe
 */
@Slf4j
public class ChannelManager implements Endpoint {
    private volatile boolean                 started = false;
    /**
     * 后端ID
     */
    private String                           id;
    /**
     * 所有通道
     */
    private Map<ChannelId, InternalChannel>  allChannels;
    /**
     * 发布中心
     */
    private MessageCenter messageCenter;
    /**
     * channel注销消息ACKtopic
     */
    private String                           channelChangeAckTopic;
    /**
     * channel注销消息topic
     */
    private String                           channelChangeTopic;
    /**
     * 通道监听
     */
    private CustomMessageListener<ChannelId> channelChangeListener;
    /**
     * 用户数据监听
     */
    private DataListener                     listener;
    /**
     * 前端管理
     */
    private BalanceManager                   balanceManager;
    /**
     * 序列化器
     */
    private List<Serializer>                 serializers;

    /**
     * 默认构造器
     *
     * @param id          后端ID
     * @param environment 环境信息
     */
    public ChannelManager(String id, Environment environment) {
        ClusterConfig clusterConfig = environment.get(Const.CLUSTER_CONFIG);
        this.id = id;
        this.balanceManager = new BalanceManager(environment);
        this.messageCenter = environment.get(Const.MSG_CENTER);
        this.channelChangeAckTopic = clusterConfig.getChannelChangeAckTopic();
        this.channelChangeTopic = clusterConfig.getChannelChangeTopic();
        this.allChannels = new ConcurrentHashMap<>();

        this.channelChangeListener = new CustomMessageListener<ChannelId>() {
            @Override
            public void onMessage(byte[] channel, ChannelId message) {
                log.debug("收到channel [{}] 关闭广播", message);
                if (message == null) {
                    log.warn("广播的channel ID为null，不处理");
                    return;
                }

                //ACK响应
                messageCenter.pub(channelChangeAckTopic + "/" + message.getChannel() + "/"
                                  + message.getBalanceId() + "/" + id,
                    message.getChannel());

                InternalChannel internalChannel = allChannels.remove(message);
                if (internalChannel != null) {
                    log.debug("channel当前存在，关闭channel[{}]", message);
                    internalChannel.closeInternal();
                }
            }

            @Override
            public Class<ChannelId> resolveMessageType() {
                return ChannelId.class;
            }
        };
        this.listener = this::write;
        this.serializers = environment.get(Const.SERIALIZER_LIST);
    }

    /**
     * 清空指定的前端所有的channel
     *
     * @param info 对应的前端的信息
     */
    public void clearChannel(BalanceInfo info) {
        allChannels.forEach((id, channel) -> {
            if (channel.getId().getBalanceId().equals(info.getId())) {
                log.debug("清除balance [{}] 下的channel [{}]", info.getId(), id);
                InternalChannel internalChannel = allChannels.remove(id);
                if (internalChannel != null) {
                    internalChannel.closeInternal();
                }
            }
        });
    }

    @Override
    public synchronized void start() throws SystemException {
        if (started) {
            log.debug("当前channel管理器已经启动，不能重复启动");
            return;
        }
        messageCenter.register(channelChangeTopic + "/" + id, channelChangeListener);
        balanceManager.start();
        balanceManager.addCloseListener(this::clearChannel);
        started = true;
    }

    @Override
    public synchronized void shutdown() throws SystemException {
        if (!started) {
            log.debug("当前channel管理器已经关闭，不能重复启动");
            return;
        }
        messageCenter.unregister(channelChangeTopic, channelChangeListener);
        balanceManager.shutdown();
        started = false;
    }

    /**
     * 根据channel信息获取channel（只应该由系统内调用，只调用一次，并且在生成session的时候调用）
     *
     * @param channel   channel的ID
     * @param balanceId 前端的ID
     * @param remoteAdd channel的客户端IP
     * @param port      channel的客户端的端口
     * @return 不会返回null
     */
    public ChannelProxy getChannel(String channel, String balanceId, String remoteAdd, int port) {
        log.debug("获取channel：{}:{}:{}:{}", channel, balanceId, remoteAdd, port);
        ChannelId id = new ChannelId(channel, balanceId);
        InternalChannel internalChannel = allChannels.get(id);
        if (internalChannel != null) {
            log.debug("当前缓存中有对应的channel");
            return internalChannel;
        } else {
            log.debug("当前缓存中没有对应的channel，构建一个");
            internalChannel = new InternalChannel(channel, balanceId, remoteAdd, port, listener,
                balanceManager.getBalance(balanceId).getTopic(), serializers);
            allChannels.put(id, internalChannel);
            return internalChannel;
        }
    }

    /**
     * 主动往前端写数据
     *
     * @param data    要写的数据
     * @param topic   要写入的topic
     * @param channel 对应的通道
     * @return 写入状态
     */
    private ProtocolFuture write(byte[] data, String topic, String channel) {
        log.debug("后端主动往前端{}发送消息{}", topic, data);
        DataMsg dataMsg = new DataMsg();
        ProtocolData protocolData = new ProtocolData();
        long time = System.currentTimeMillis();

        Datagram datagram = DatagramUtil.build(data, (byte) 4, (byte) 1);
        log.debug("生成的数据报为：{}", datagram);
        protocolData.setData(datagram.getData());
        protocolData.setChannel(channel);
        protocolData.setReqTime(time);
        dataMsg.setId(Tools.createUUID());
        dataMsg.setTopic(topic);
        dataMsg.setCreateTime(time);
        dataMsg.setData(protocolData);
        dataMsg.setSrc(id);
        log.debug("生成的待发送数据为：{}，将数据发送到：{}", dataMsg, topic);
        messageCenter.pub(topic, dataMsg);
        return ProtocolFuture.SUCCESS;
    }

    /**
     * 内部channel实现（只有write、id、getRemoteHost、getPort、isClosed、addCloseCallback方法可以使用，同时write方法
     * 无论写入是否成功都返回成功ProtocolFuture.SUCCESS）
     * <p>
     * 使用该channel的时候应该先注册关闭监听，然后调用isClose方法判断是否关闭，如果关闭那么就认为channel失效
     */
    @Slf4j
    private static class InternalChannel implements ChannelProxy {
        private static final JsonParser    parser = JsonParser.getInstance();
        private ChannelId                  id;
        private String                     remoteAdd;
        private int                        port;
        private List<ChannelCloseCallback> callbacks;
        private volatile boolean           close  = false;
        /**
         * 数据监听器，当用户调用write后实际会调用该方法
         */
        private DataListener               listener;
        private String                     topic;
        private List<Serializer>           serializers;

        /**
         * 虚拟channel的构造器
         *
         * @param channel   该虚拟channel对应的实际channel的ID
         * @param balanceId 该channel的归属前端ID
         * @param remoteAdd 该channel的客户端IP
         * @param port      该channel的客户端端口
         * @param listener  数据监听（用于将用户的数据发送到前端）
         * @param topic     该channel对应的前端监听的topic
         */
        private InternalChannel(@NonNull String channel, @NonNull String balanceId,
                                @NonNull String remoteAdd, int port, @NonNull DataListener listener,
                                @NonNull String topic, @NonNull List<Serializer> serializers) {
            this.id = new ChannelId(channel, balanceId);
            this.remoteAdd = remoteAdd;
            this.port = port;
            this.callbacks = new CopyOnWriteArrayList<>();
            this.listener = listener;
            this.topic = topic;
            this.serializers = serializers;
        }

        @Override
        public ProtocolFuture write(String invoke, String data) {
            InterfaceData interfaceData = new InterfaceData(Tools.createUUID(), invoke, data);
            return listener.write(write(interfaceData).getBytes(), topic, id.getChannel());
        }

        private String write(Object obj) {
            if (serializers.isEmpty()) {
                return parser.toJson(obj);
            } else {
                for (Serializer serializer : serializers) {
                    if (serializer.writeable(obj)) {
                        return new String(serializer.write(obj));
                    }
                }
                return parser.toJson(obj);
            }
        }

        @Override
        public String id() {
            return id.getChannel();
        }

        @Override
        public String getRemoteHost() {
            return remoteAdd;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public boolean isClosed() {
            return close;
        }

        /**
         * 当收到注销事件时调用该方法
         */
        void closeInternal() {
            close = true;
            callbacks.forEach(callback -> {
                try {
                    callback.close(id.getChannel());
                } catch (Throwable e) {
                    log.debug("调用channel [{}] 的关闭回调 [{}] 时出错", id, this, e);
                }
            });
        }

        ChannelId getId() {
            return this.id;
        }

        @Override
        public void addCloseCallback(ChannelCloseCallback callback) {
            callbacks.add(callback);
        }
    }
}
