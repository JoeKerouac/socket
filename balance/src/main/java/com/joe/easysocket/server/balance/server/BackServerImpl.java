package com.joe.easysocket.server.balance.server;

import com.joe.easysocket.server.balance.Config;
import com.joe.easysocket.server.balance.protocol.listener.ProtocolDataListener;
import com.joe.easysocket.server.common.info.BackServerInfo;
import com.joe.easysocket.server.common.msg.CustomMessageListener;
import com.joe.easysocket.server.common.msg.DataMsg;
import com.joe.easysocket.server.common.msg.PublishCenter;
import lombok.extern.slf4j.Slf4j;

/**
 * 虚拟后端（当前暂未对ACK进行判断）
 *
 * @author joe
 */
@Slf4j
public class BackServerImpl implements BackServer {
    /**
     * 后端信息
     */
    private BackServerInfo info;
    /**
     * 发布中心
     */
    private final PublishCenter publishCenter;
    /**
     * 后端消息响应监听
     */
    private final ProtocolDataListener protocolDataListener;
    /**
     * 真实后端的根topic
     */
    private final String topic;
    /**
     * 后端消息响应代理监听
     */
    private CustomMessageListener<DataMsg> listener;
    /**
     * 记录后端是否启动
     */
    private boolean started = false;
    /**
     * 消息回复的topic
     */
    private String msgResp;
    /**
     * 前端ID
     */
    private String id;


    /**
     * 虚拟后端构造器
     *
     * @param config               配置
     * @param info                 要代理的实际后端的信息
     * @param protocolDataListener 消息回复监听（后端处理完毕后的回复）
     * @param id                   前端的ID
     */
    public BackServerImpl(Config config, BackServerInfo info, ProtocolDataListener protocolDataListener, String id) {
        this.info = info;
        this.publishCenter = config.getClusterConfig().getPublishCenter();
        this.protocolDataListener = protocolDataListener;
        this.topic = info.getTopic();
        this.id = id;
        this.msgResp = config.getClusterConfig().getMsgResp();
    }

    @Override
    public void write(DataMsg msg) {
        log.debug("将消息{}发布到{}", msg, topic);
        publishCenter.pub(topic, msg);
    }

    @Override
    public void update(BackServerInfo info) {
        log.debug("更新虚拟后端{}的信息，更新后为：{}", this.info, info);
        this.info = info;
    }

    @Override
    public synchronized void start() {
        log.debug("启动虚拟后端：{}", info);
        if (started) {
            log.warn("虚拟后端{}已经启动", info);
            return;
        }

        log.debug("为虚拟后端{}添加消息回复监听", info);
        listener = new CustomMessageListener<DataMsg>() {
            @Override
            public void onMessage(byte[] channel, DataMsg message) {
                read(message);
            }

            @Override
            public Class<DataMsg> resolveMessageType() {
                return DataMsg.class;
            }
        };
        String msgResp = this.msgResp + "/" + id;
        log.debug("注册后端服务响应监听，监听topic：{}", msgResp);
        publishCenter.register(msgResp, listener);
        started = true;
        log.debug("虚拟后端{}的消息回复监听添加成功", info);
        log.debug("虚拟后端{}启动成功", info);
    }

    @Override
    public synchronized void shutdown() {
        log.debug("关闭虚拟后端{}", info);
        if (!started) {
            log.warn("虚拟后端{}已经关闭，请勿重复关闭", info);
            return;
        }
        log.debug("删除虚拟服务器{}对后端的消息响应监听", info);
        publishCenter.unregister(topic + msgResp, listener);
        log.debug("虚拟服务器{}对后端的消息响应监听删除成功", info);
        started = false;
        log.debug("虚拟后端{}关闭成功", info);
    }

    @Override
    public void read(DataMsg msg) {
        try {
            log.debug("虚拟服务器{}接收到回复：{}", info, msg);
            long now = System.currentTimeMillis();
            log.debug("本次请求耗时{}ms", (now - msg.getCreateTime()));
            protocolDataListener.exec(msg.getData());
        } catch (Exception e) {
            log.error("虚拟服务器{}的消息{}处理失败", info, msg);
        }
    }

    @Override
    public String getName() {
        return info.getName();
    }

    @Override
    public String getId() {
        return info.getId();
    }

    @Override
    public BackServerInfo getServerInfo() {
        return info;
    }

    @Override
    public String toString() {
        return info.toString();
    }
}
