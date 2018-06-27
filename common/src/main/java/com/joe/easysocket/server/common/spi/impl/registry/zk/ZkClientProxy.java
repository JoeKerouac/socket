package com.joe.easysocket.server.common.spi.impl.registry.zk;

import com.joe.easysocket.server.common.exception.ZKClientException;
import com.joe.easysocket.server.common.spi.NodeEvent;
import com.joe.easysocket.server.common.spi.Serializer;
import com.joe.utils.concurrent.LockService;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * proxy zkClient , thread safe.
 *
 * @author joe
 */
@Slf4j
class ZkClientProxy implements ZKClient {
    private List<Serializer> serializers;
    /**
     * real zk client
     */
    private CuratorFramework client;
    /**
     * zk client config
     */
    private ZKConfig config;
    /**
     * 节点变化监听器集合
     */
    private Map<String, CacheAdapater> pathChildrenCaches = new HashMap<>();
    /**
     * 递归节点变化监听器集合
     */
    private Map<String, CacheAdapater> treeCaches = new HashMap<>();
    private Lock listenerLock = LockService.getLock(ZKClient.class + "/listenerLock");


    /**
     * default contrastor
     *
     * @param config config is required;
     * @throws IllegalArgumentException 当config是null时抛出该异常
     */
    public ZkClientProxy(ZKConfig config) throws IllegalArgumentException {
        if (config == null) {
            throw new IllegalArgumentException("config must non null");
        }
        this.config = config;
        this.serializers = config.getSerializers() == null ? Collections.emptyList() : config.getSerializers();
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(config.getBaseSleepTimeMs(), config.getMaxRetry());
        CuratorFrameworkFactory.builder().authorization("", null).authorization("", null);
        this.client = CuratorFrameworkFactory.newClient(config.getConnectStr(), retryPolicy);
        this.client.getConnectionStateListenable().addListener(new ConnectionStateListenerAdapter(this, ((client1,
                                                                                                          newState) -> {
            if (newState.equals(com.joe.easysocket.server.common.spi.ConnectionState.CONNECTED)) {
                log.info("客户端连接成功，准备启动节点监听器");
                listenerLock.lock();
                try {
                    treeCaches.entrySet().stream().map(Map.Entry::getValue).forEach(CacheAdapater::start);
                    pathChildrenCaches.entrySet().stream().map(Map.Entry::getValue).forEach(CacheAdapater::start);
                } finally {
                    listenerLock.unlock();
                }
            }
        })));
    }

    @Override
    public synchronized void start() {
        if (client.getState().equals(CuratorFrameworkState.STARTED)) {
            log.warn("客户端已经启动，请勿重复启动客户端");
            return;
        } else {
            log.info("启动客户端...");
            client.start();
        }
    }

    @Override
    public synchronized void addListener(ZKConnectionStateListener listener) {
        if (listener == null) {
            log.warn("listener不能为null");
            return;
        }
        client.getConnectionStateListenable().addListener(new ConnectionStateListenerAdapter(this, listener));
    }

    @Override
    public synchronized void addListener(String path, boolean recursion, ChildrenCacheListener listener) throws
            Exception {
        if (!isStarted()) {
            log.warn("当前客户端未启动");
        }

        if (path == null || listener == null) {
            log.warn("path不能为null或者listener不能为null");
            return;
        }

        path = dealPath(path);

        CacheAdapater cache = recursion ? treeCaches.get(path) : pathChildrenCaches.get(path);
        if (recursion) {
            if (cache == null) {
                cache = new CacheAdapater(null, new TreeCache(client, path));
                treeCaches.put(path, cache);
            }
            cache.addListener(new ChildrenCacheListenerAdapter(this, listener));
        } else {
            if (cache == null) {
                cache = new CacheAdapater(new PathChildrenCache(client, path, true), null);
                pathChildrenCaches.put(path, cache);
            }
            cache.addListener(new ChildrenCacheListenerAdapter(this, listener));
        }
        listenerLock.lock();
        try {
            if (isStarted()) {
                log.debug("启动节点监听器");
                cache.start();
            }
        } finally {
            listenerLock.unlock();
        }
    }

    @Override
    public boolean isStarted() {
        return client.getState().equals(CuratorFrameworkState.STARTED);
    }

    public <T> String createSeq(String path, T data) {
        if (!isStarted()) {
            log.warn("当前客户端没有启动");
            return null;
        }

        if (path == null) {
            log.warn("path不能为null");
            return null;
        }

        path = dealPath(path);
        path = path.length() == 1 ? path : path + "/";

        try {
            log.debug("创建zookeeper目录：{}", path);
            client.createContainers(path);
            log.debug("zookeeper目录{}创建成功", path);
            log.debug("创建递增节点");
            String nodePath = client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path, write(data));
            log.debug("递增节点为：{}", nodePath);
            return nodePath;
        } catch (Exception e) {
            if (e instanceof ZKClientException) {
                throw (ZKClientException) e;
            } else {
                log.error("往{}添加数据{}时发生异常", path, data, e);
                throw new ZKClientException("添加数据失败", e);
            }
        }
    }

    @Override
    public <T> boolean create(String path, T data) {
        if (!isStarted()) {
            log.warn("当前客户端没有启动");
            return false;
        }

        if (path == null) {
            log.warn("path不能为null");
            return false;
        }

        path = dealPath(path);

        try {
            createRoot(path);
            update(path, data);
            log.debug("往{}添加数据{}成功", path, data);
            return true;
        } catch (Exception e) {
            if (e instanceof ZKClientException) {
                throw (ZKClientException) e;
            } else {
                log.error("往{}添加数据{}时发生异常", path, data, e);
                throw new ZKClientException("添加数据失败", e);
            }
        }
    }

    @Override
    public boolean delete(String path) {
        if (!isStarted()) {
            log.warn("当前客户端没有启动");
            return false;
        }

        if (path == null) {
            log.warn("path不能为null");
            return false;
        }

        path = dealPath(path);

        if (path.startsWith("/zookeeper") || path.equals("/")) {
            log.warn("节点'/zookeeper'、节点'/'不能删除");
            return false;
        }

        try {
            client.delete().forPath(path);
            log.debug("删除节点{}成功", path);
            return true;
        } catch (Exception e) {
            log.error("删除数据{}失败", path, e);
            throw new ZKClientException("删除数据失败", e);
        }
    }

    @Override
    public boolean deleteAll(String root) {
        log.debug("删除节点{}下的所有数据", root);
        if (!isStarted()) {
            log.warn("当前客户端没有启动");
            return false;
        }

        if (root == null) {
            log.warn("path不能为null");
            return false;
        }

        root = dealPath(root);

        try {
            List<String> list = client.getChildren().forPath(root);
            for (String p : list) {
                deleteAll((root.endsWith("/") ? root : root + "/") + p);
            }
            delete(root);
            log.debug("节点{}下的所有数据删除成功", root);
            return true;
        } catch (Exception e) {
            if (e instanceof ZKClientException) {
                throw (ZKClientException) e;
            } else {
                log.error("删除节点[{}]下的所有数据失败", root, e);
                throw new ZKClientException("删除节点[" + root + "]下所有数据失败", e);
            }
        }
    }

    @Override
    public <T> boolean update(String path, T data) {
        if (!isStarted()) {
            log.warn("当前客户端没有启动");
            return false;
        }

        if (path == null) {
            log.warn("path不能为null");
            return false;
        }

        path = dealPath(path);

        try {
            client.setData().forPath(path, write(data));
            log.debug("更新节点{}数据成功，当前节点{}的数据为：{}", path, path, data);
            return true;
        } catch (Exception e) {
            log.error("删除数据{}失败", path, e);
            throw new ZKClientException("删除数据失败", e);
        }
    }

    @Override
    public boolean exists(String path) {
        log.debug("检查节点{}是否存在", path);
        if (!isStarted()) {
            log.warn("当前客户端没有启动");
            return false;
        }

        if (path == null) {
            log.warn("path不能为null");
            return false;
        }

        path = dealPath(path);

        try {
            return client.checkExists().forPath(path) != null;
        } catch (Exception e) {
            log.error("检查[{}]是否存在异常", path, e);
            throw new ZKClientException("检查[" + path + "]是否存在失败", e);
        }
    }

    @Override
    public <T> T getData(String path, Class<T> clazz) {
        log.debug("获取路径{}下的数据", path);

        if (path == null || clazz == null) {
            log.warn("path不能为null或者类型不能为null");
            return null;
        }

        path = dealPath(path);

        try {
            byte[] data = client.getData().forPath(path);
            return read(data, clazz);
        } catch (Exception e) {
            log.error("读取数据{}失败", path);
            throw new ZKClientException("读取数据失败", e);
        }
    }

    @Override
    public List<String> getAll() {
        return getAll("", Collections.singletonList("/"));
    }

    @Override
    public List<String> getAll(String path) {
        if (path == null) {
            log.warn("path不能为null");
            return Collections.emptyList();
        }
        return getAll("", Collections.singletonList(dealPath(path)));
    }

    @Override
    public void close() throws IOException {
        if (isStarted()) {
            log.debug("关闭ZKClient");
            client.close();
        } else {
            log.warn("当前ZKClient未开启，不能关闭");
        }
    }

    /**
     * 创建根路径
     *
     * @param path 路径
     */
    private void createRoot(String path) {
        log.debug("创建路径{}", path);
        if (path.split("/").length == 1) {
            return;
        } else {
            try {
                client.createContainers(path);
            } catch (Exception e) {
                throw new ZKClientException("创建路径" + path, e);
            }
        }
    }

    /**
     * 处理路径
     *
     * @param path 指定路径
     * @return 处理后的路径
     */
    private String dealPath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 获取指定节点集合的所有子节点
     *
     * @param paths 节点集合
     * @return 指定节点集合对应的所有字节点
     * @throws ZKClientException 遍历异常
     */
    private List<String> getAll(String root, List<String> paths) throws ZKClientException {
        log.debug("获取节点集合{}的所有字节点", paths);
        if (paths.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> all = new ArrayList<>();

        paths.parallelStream().forEach(path -> {
            try {
                path = root + path;

                log.debug("获取节点{}的所有一级子节点", path);
                List<String> childs = client.getChildren().forPath(path);
                log.debug("节点{}的所有一级子节点为：{}", path, childs);

                String newRoot = path.endsWith("/") ? path : path + "/";

                if (!childs.isEmpty()) {
                    all.addAll(childs.parallelStream().map(child -> newRoot + child).collect(Collectors.toList()));

                    all.addAll(getAll(newRoot, childs));
                }
            } catch (Exception e) {
                log.warn("获取节点{}的字节点时发生异常", path, e);
                if (e instanceof ZKClientException) {
                    throw (ZKClientException) e;
                } else {
                    throw new ZKClientException("遍历所有字节点时发生异常", e);
                }
            }
        });
        log.debug("节点集合{}对应的所有子节点为：{}", paths, all);
        return all;
    }

    private byte[] write(Object data) {
        log.debug("开始序列化数据{}", data);
        if (data == null) {
            return null;
        }
        for (Serializer serializer : serializers) {
            if (serializer.writeable(data.getClass())) {
                log.debug("使用序列化器{}序列化数据{}", serializer.getClass(), data);
                return serializer.write(data);
            }
        }
        log.debug("对于数据[{}]没有可用的序列器", data);
        return null;
    }

    /**
     * 反序列化数据
     *
     * @param data  数据
     * @param clazz 数据类型Class
     * @param <T>   数据类型
     * @return 反序列化的数据
     */
    private <T> T read(byte[] data, Class<T> clazz) {
        log.debug("开始反序列化数据，数据类型为：{}，数据为：{}", clazz, data);

        if (data == null) {
            return null;
        }

        for (Serializer serializer : serializers) {
            if (serializer.readable(data.getClass())) {
                log.debug("使用反序列化器{}反序列化数据{}", serializer.getClass(), data);
                return serializer.read(data, clazz);
            }
        }
        log.debug("对于数据[{}]、类型[{}]没有可用的序列化器", data, clazz);
        return null;
    }

    /**
     * 节点缓存的适配器
     */
    private static class CacheAdapater {
        private PathChildrenCache pathChildrenCache;
        private TreeCache treeCache;
        private boolean started = false;

        /**
         * 节点缓存构造器
         *
         * @param pathChildrenCache PathChildrenCache
         * @param treeCache         TreeCache
         * @throws IllegalArgumentException 同时传入PathChildrenCache和TreeCache时抛出该异常
         */
        CacheAdapater(PathChildrenCache pathChildrenCache, TreeCache treeCache) throws IllegalArgumentException {
            if (!(pathChildrenCache == null ^ treeCache == null)) {
                throw new IllegalArgumentException("不能同时传入PathChildrenCache和TreeCache");
            }
            this.pathChildrenCache = pathChildrenCache;
            this.treeCache = treeCache;
        }

        /**
         * 判断当前cache是否启动
         *
         * @return 返回true表示已经启动
         */
        public boolean isStarted() {
            return started;
        }

        /**
         * 启动
         *
         * @throws RuntimeException 启动异常
         */
        public synchronized void start() throws RuntimeException {
            if (isStarted()) {
                log.warn("当前节点监听器已经启动");
                return;
            }
            log.debug("准备启动节点监听器");
            try {
                if (this.pathChildrenCache != null) {
                    this.pathChildrenCache.start();
                } else {
                    this.treeCache.start();
                }
            } catch (Exception e) {
                throw new RuntimeException("节点监听器启动异常", e);
            }
            started = true;
        }

        /**
         * 添加监听器
         *
         * @param listener 监听器
         */
        public void addListener(ChildrenCacheListenerAdapter listener) {
            if (this.pathChildrenCache != null) {
                this.pathChildrenCache.getListenable().addListener(listener);
            } else {
                this.treeCache.getListenable().addListener(listener);
            }
        }
    }


    /**
     * {@link org.apache.curator.framework.state.ConnectionStateListener}的适配器
     */
    private static class ConnectionStateListenerAdapter implements org.apache.curator.framework.state
            .ConnectionStateListener {
        private ZKConnectionStateListener listener;
        private ZkClientProxy client;

        ConnectionStateListenerAdapter(ZkClientProxy client, ZKConnectionStateListener listener) {
            this.client = client;
            this.listener = listener;
        }

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            log.debug("客户端{}状态发生变化，当前客户端状态为：{}", client, newState);
            listener.stateChanged(this.client, com.joe.easysocket.server.common.spi.ConnectionState.valueOf
                    (newState.name()));
        }
    }

    /**
     * {@link PathChildrenCacheListener}的适配器
     */
    private static class ChildrenCacheListenerAdapter implements PathChildrenCacheListener, TreeCacheListener {

        private ChildrenCacheListener childrenCacheListener;
        private ZkClientProxy client;

        ChildrenCacheListenerAdapter(ZkClientProxy client, ChildrenCacheListener childrenCacheListener) {
            this.client = client;
            this.childrenCacheListener = childrenCacheListener;
        }

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            PathChildrenCacheEvent.Type type = event.getType();

            switch (type) {
                case CHILD_ADDED:
                case CHILD_UPDATED:
                case CHILD_REMOVED:
                    log.info("接收到节点变化事件{}", event);
                    ChildData d = event.getData();
                    com.joe.easysocket.server.common.spi.ChildData data = new com.joe.easysocket.server.common.spi
                            .ChildData(d.getPath(), d.getData());
                    NodeEvent internalEvent = new NodeEvent(NodeEvent.Type.valueOf(type
                            .name().replace("CHILD", "NODE")), data);
                    childrenCacheListener.childEvent(this.client, internalEvent);
                    break;
                default:
                    log.debug("不支持事件{}", event);
                    break;
            }
        }

        @Override
        public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
            TreeCacheEvent.Type type = event.getType();

            switch (type) {
                case NODE_ADDED:
                case NODE_UPDATED:
                case NODE_REMOVED:
                    log.info("接收到节点变化事件{}", event);
                    ChildData d = event.getData();
                    com.joe.easysocket.server.common.spi.ChildData data = new com.joe.easysocket.server.common.spi
                            .ChildData(d.getPath(), d.getData());
                    NodeEvent internalEvent = new NodeEvent(NodeEvent.Type.valueOf(type
                            .name()), data);
                    childrenCacheListener.childEvent(this.client, internalEvent);
                    break;
                default:
                    log.debug("不支持事件{}", event);
                    break;
            }
        }
    }
}