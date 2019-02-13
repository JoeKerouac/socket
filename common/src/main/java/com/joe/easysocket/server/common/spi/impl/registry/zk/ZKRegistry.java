package com.joe.easysocket.server.common.spi.impl.registry.zk;

import java.io.IOException;
import java.util.List;

import com.joe.easysocket.server.common.config.Const;
import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.spi.ConnectionStateListener;
import com.joe.easysocket.server.common.spi.NodeListener;
import com.joe.easysocket.server.common.spi.Registry;

/**
 * 依托zookeeper实现的注册中心，需要zookeeper3.5及以上版本
 *
 * @author joe
 */
public class ZKRegistry implements Registry {
    private ZKClient client;

    @Override
    public void start() {
        client.start();
    }

    @Override
    public void addListener(ConnectionStateListener listener) {
        client.addListener(((client1, newState) -> listener.stateChanged(this, newState)));
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @Override
    public <T> String add(String path, T data) {
        return client.createSeq(path, data);
    }

    @Override
    public boolean delete(String path) {
        return client.delete(path);
    }

    @Override
    public boolean deleteAll(String path) {
        return client.deleteAll(path);
    }

    @Override
    public <T> boolean update(String path, T data) {
        return client.update(path, data);
    }

    @Override
    public boolean exists(String path) {
        return client.exists(path);
    }

    @Override
    public <T> T getData(String path, Class<T> clazz) {
        return client.getData(path, clazz);
    }

    @Override
    public List<String> getAll() {
        return client.getAll();
    }

    @Override
    public void addListener(String path, NodeListener listener) throws Exception {
        client.addListener(path, true, ((client1, event) -> listener.listen(this, event)));
    }

    @Override
    public void setProperties(Environment environment) {
        ZKConfig config = environment.get(Const.ZK_CONFIG);
        if (config == null) {
            throw new NullPointerException("环境中没有zkConfig信息，使用ZKRegistry需要提供一个zkConfig配置");
        }
        this.client = ZKClient.build(config);
    }
}
