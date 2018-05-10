package com.joe.easysocket.server.backserver.mvc.impl.context.session;

import com.joe.easysocket.server.backserver.Config;
import com.joe.easysocket.server.backserver.manager.ChannelManager;
import com.joe.easysocket.server.backserver.mvc.context.Session;
import com.joe.easysocket.server.backserver.mvc.context.SessionManager;
import com.joe.utils.concurrent.LockService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Session管理器
 *
 * @author joe
 */
@Slf4j
public class SessionManagerImpl implements SessionManager {
    private volatile boolean started = false;
    private Map<String, LocalSession> cache;
    /**
     * channel管理器
     */
    private ChannelManager channelManager;


    /**
     * 默认构造器
     *
     * @param id     后端的ID
     * @param config 后端配置
     */
    public SessionManagerImpl(String id, Config config) {
        this.channelManager = new ChannelManager(id, config);
    }

    @Override
    public synchronized void start() {
        if (started) {
            log.debug("session管理器已经启动，请勿重复启动");
            return;
        }
        cache = new HashMap<>();
        channelManager.start();
        started = true;
    }

    @Override
    public synchronized void shutdown() {
        if (!started) {
            log.debug("session管理器已经关闭，请勿重复启动");
            return;
        }
        cache.clear();
        channelManager.shutdown();
        started = false;
    }

    @Override
    public Session get(String channel, String balanceId, int port, String host) {
        if (cache.get(channel) == null) {
            LockService.lock(channel);
            cache.computeIfAbsent(channel, c -> new LocalSession(c, host, port, channelManager.getChannel(c,
                    balanceId, host, port)));
            LockService.unlock(channel);
        }
        return cache.get(channel);
    }

    @Override
    public Session remove(String channel) {
        return (channel == null || cache == null) ? null : cache.get(channel);
    }
}
