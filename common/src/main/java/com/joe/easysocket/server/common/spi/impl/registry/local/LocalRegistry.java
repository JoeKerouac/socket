package com.joe.easysocket.server.common.spi.impl.registry.local;

import com.joe.easysocket.server.common.spi.*;
import com.joe.utils.collection.Tree;
import com.joe.utils.common.StringUtils;
import com.joe.utils.parse.json.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地注册中心，不能用于分布式项目，测试使用
 *
 * @author joe
 * @version 2018.04.11 18:13
 */
public class LocalRegistry implements Registry {
    private static final JsonParser PARSER = JsonParser.getInstance();
    private Tree registry;
    private Tree<List<NodeListener>> listeners;
    private AtomicLong counter;

    public LocalRegistry() {
        registry = new Tree();
        this.listeners = new Tree<>();
        this.counter = new AtomicLong(0);
    }


    @Override
    public void start() {

    }

    @Override
    public void addListener(ConnectionStateListener listener) {

    }

    @Override
    public <T> String add(String path, T data) {
        path = StringUtils.trim(path, "/") + "/" + counter.getAndAdd(1);
        registry.add(path, PARSER.toJson(data));
        return path;
    }

    @Override
    public boolean delete(String path) {
        registry.delete(path);
        return true;
    }

    @Override
    public boolean deleteAll(String path) {
        registry.delete(path, true);
        return true;
    }

    @Override
    public <T> boolean update(String path, T data) {
        registry.updateData(path, data);
        return true;
    }

    @Override
    public boolean exists(String path) {
        return registry.exit(path);
    }

    @Override
    public <T> T getData(String path, Class<T> clazz) {
        return (T) registry.getData(path);
    }

    @Override
    public List<String> getAll() {
        throw new RuntimeException("该功能尚未实现");
    }

    @Override
    public void addListener(String path, NodeListener listener) throws Exception {
        List<NodeListener> list = listeners.getData(path);
        if (list == null) {
            synchronized (this) {
                list = listeners.getData(path);
                if (list == null) {
                    list = new ArrayList<>();
                    listeners.add(path, list);
                }
            }
        }
        list.add(listener);
        String data = getData(path, String.class);
        if (data != null) {
            listener.listen(this, new NodeEvent(NodeEvent.Type.NODE_ADDED, new ChildData(path, data.getBytes())));
        }
    }

    @Override
    public void close() throws IOException {
        registry.clear();
    }
}
