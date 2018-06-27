package com.joe.easysocket.server.balance.strategy;

import com.joe.easysocket.server.balance.server.BackServer;
import com.joe.easysocket.server.common.exception.SystemException;
import com.joe.easysocket.server.common.info.BackServerInfo;
import com.joe.easysocket.server.common.msg.DataMsg;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 轮询策略
 *
 * @author joe
 */
@Slf4j
public class PollLoadSrategy implements LoadStrategy {
    private Deque<DataMsg> deque = new LinkedBlockingDeque<>();
    private List<BackServer> backServers = new CopyOnWriteArrayList<>();
    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    private BackServer DEFAULT = new BackServer() {
        @Override
        public void write(DataMsg msg) {
            if (!backServers.isEmpty()) {
                log.warn("当前负载器已经有实际的负载了，请使用调用next获取最新的后端");
                next().write(msg);
            }
            if (deque.offer(msg)) {
                log.debug("数据{}加入队列成功", msg);
            } else {
                log.warn("数据{}加入队列失败", msg);
            }
        }

        @Override
        public BackServer update(BackServerInfo serverInfo) {
            return this;
        }

        @Override
        public void start() throws SystemException {

        }

        @Override
        public void shutdown() throws SystemException {

        }

        @Override
        public void read(DataMsg msg) {

        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public BackServerInfo getServerInfo() {
            return null;
        }
    };

    @Override
    public BackServer next() {
        if (backServers.isEmpty()) {
            log.debug("当前没有注册后端处理器，使用默认后端处理器");
            return DEFAULT;
        } else {
            log.debug("当前有后端处理器，从中选择一个");
            int i = atomicInteger.getAndAdd(1);

            //扩容
            int cap = 3;
            if (Integer.MAX_VALUE - cap < i || i < 0) {
                synchronized (atomicInteger) {
                    log.debug("当前index过大，需要重置索引");
                    if (atomicInteger.get() > Integer.MAX_VALUE - cap || atomicInteger.get() < 0) atomicInteger.set(0);
                }
            }

            if (i < 0) {
                i = atomicInteger.getAndAdd(1);
            }

            return backServers.get(i % backServers.size());
        }
    }

    @Override
    public void addLoad(BackServer server) {
        log.debug("添加负载{}：{}", server.getId(), server);
        backServers.add(server);
        if (backServers.size() == 1) {
            DataMsg msg;
            while ((msg = deque.pollFirst()) != null) {
                log.debug("当前队列中有消息未读，将消息{}发送到后端{}：{}", msg, server.getId(), server);
                server.write(msg);
            }
        }
    }

    @Override
    public void removeLoad(String id) {
        log.debug("删除server{}", id);
        for (int i = 0; i < backServers.size(); i++) {
            if (id.equals(backServers.get(i).getId())) {
                backServers.remove(i);
                return;
            }
        }
        log.debug("要删除的节点{}不存在", id);
    }

    @Override
    public void update(String id, BackServerInfo info) {
        backServers.stream().filter(backServer -> id.equals(backServer.getId())).forEach(backServer -> backServer
                .update(info));
    }

    @Override
    public void clear() {
        backServers.clear();
    }
}
