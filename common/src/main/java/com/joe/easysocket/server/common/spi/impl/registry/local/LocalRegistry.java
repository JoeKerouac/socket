package com.joe.easysocket.server.common.spi.impl.registry.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import com.joe.easysocket.server.common.spi.*;
import com.joe.utils.collection.Tree;
import com.joe.utils.common.StringUtils;
import com.joe.utils.serialize.json.JsonParser;

/**
 * 本地注册中心，不能用于分布式项目
 * <p>
 * <strong>极度不稳定，仅用于测试使用，正式使用请用{@link com.joe.easysocket.server.common.spi.impl.registry.zk.ZKRegistry ZKRegistry
 * }</strong>
 *
 * @author joe
 * @version 2018.04.11 18:13
 */
public class LocalRegistry implements Registry {
    private static final JsonParser PARSER = JsonParser.getInstance();
    private Tree                     registry;
    private Tree<List<NodeListener>> listeners;
    private AtomicLong               counter;

    public LocalRegistry() {
        this.registry = new Tree();
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
        String pathPre = StringUtils.trim(path, "/");
        path = pathPre + "/" + counter.getAndAdd(1);
        String dataStr = PARSER.toJson(data);
        registry.add(path, dataStr);

        List<NodeListener> listenerList = listeners.getData(pathPre);
        if (listenerList != null && !listenerList.isEmpty()) {
            listenerList.stream()
                .forEach(listener -> listener.listen(this, new NodeEvent(NodeEvent.Type.NODE_ADDED,
                    new ChildData("/" + pathPre, dataStr.getBytes()))));
        }
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
        List<String> datas = registry.getAllChildData(path);
        if (!datas.isEmpty()) {
            datas.forEach(data -> listener.listen(this,
                new NodeEvent(NodeEvent.Type.NODE_ADDED, new ChildData(path, data.getBytes()))));
        }
    }

    @Override
    public void close() throws IOException {
        registry.clear();
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
