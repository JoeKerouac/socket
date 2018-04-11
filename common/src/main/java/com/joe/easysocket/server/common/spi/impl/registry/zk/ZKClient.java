package com.joe.easysocket.server.common.spi.impl.registry.zk;

import java.io.Closeable;
import java.util.List;

/**
 * @author joe
 */
public interface ZKClient extends Closeable {
    /**
     * 启动客户端，如果客户端已经启动那么该方法将不会做任何事
     */
    void start();

    /**
     * 添加客户端状态监听
     *
     * @param listener 监听器
     */
    void addListener(ZKConnectionStateListener listener);

    /**
     * 添加节点监听器
     *
     * @param path      要监听的根节点
     * @param recursion 是否递归监听，true表示递归监听，false表示只监听指定根节点，不监听子节点
     * @param listener  监听器
     */
    void addListener(String path, boolean recursion, ChildrenCacheListener listener) throws
            Exception;

    /**
     * 当前客户端是否启动
     *
     * @return 返回true表示启动，否则表示未启动
     */
    boolean isStarted();

    /**
     * 创建临时节点，在该节点下生成一个目录（该目录序号递增）
     *
     * @param path 节点
     * @param data 数据
     * @param <T>  数据类型
     * @return 生成的临时目录path
     */
    <T> String createSeq(String path, T data);

    /**
     * 创建节点（当指定节点存在时抛出异常）
     *
     * @param path 节点路径
     * @param data 节点数据
     * @param <T>  数据类型
     * @return 返回true表示添加节点成功
     */
    <T> boolean create(String path, T data);

    /**
     * 删除节点
     *
     * @param path 节点路径
     * @return 返回true表示删除成功
     */
    boolean delete(String path);

    /**
     * 删除节点下的所有数据
     *
     * @param path 节点
     * @return 返回true表示删除成功
     */
    boolean deleteAll(String path);

    /**
     * 更新节点数据
     *
     * @param path 节点路径
     * @param data 数据
     * @param <T>  数据类型
     * @return 返回true表示成功
     */
    <T> boolean update(String path, T data);

    /**
     * 检查指定节点是否存在
     *
     * @param path 节点路径
     * @return 返回true表示指定节点存在
     */
    boolean exists(String path);

    /**
     * 获取节点数据
     *
     * @param path  节点路径
     * @param clazz 数据类型的Class
     * @param <T>   数据类型
     * @return 节点对应的数据
     */
    <T> T getData(String path, Class<T> clazz);

    /**
     * 获取所有节点的集合
     *
     * @return 所有节点集合
     */
    List<String> getAll();

    /**
     * 获取指定节点下的所有子节点
     *
     * @param path 指定节点
     * @return 指定节点下的所有子节点
     */
    List<String> getAll(String path);

    /**
     * 构建默认ZKClient
     *
     * @param config zookeeper配置
     * @return 默认ZKClient
     */
    static ZKClient build(ZKConfig config) {
        return new ZkClientProxy(config);
    }
}
