package com.joe.easysocket.server.common.spi;


import com.joe.easysocket.server.common.registry.ConnectionStateListener;
import com.joe.easysocket.server.common.registry.NodeListener;
import com.joe.easysocket.server.common.registry.zk.ZKConfig;
import com.joe.easysocket.server.common.registry.zk.ZKRegistry;

import java.io.Closeable;
import java.util.List;

/**
 * 注册中心
 *
 * @author joe
 */
public interface Registry extends Closeable {
    /**
     * 启动注册中心，如果注册中心已经启动那么该方法将不会做任何事
     */
    void start();

    /**
     * 添加客户端状态监听
     *
     * @param listener 监听器
     */
    void addListener(ConnectionStateListener listener);

    /**
     * 注册数据（会在指定路径下生成一个节点，同时返回该节点的路径，例如当path是"abc"是将有可能返回"/abc/00001"或
     * 者"/abc/0000"等，最后数字字符串是自动生成的，当该注册中心关闭时通过该注册中心注册的数据全部删除）
     *
     * @param path 路径
     * @param data 数据
     * @param <T>  数据类型
     * @return 生成的节点的路径
     */
    <T> String add(String path, T data);

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
     * 添加节点监听器
     *
     * @param path     节点路径
     * @param listener 监听器
     * @throws Exception 添加监听器失败
     */
    void addListener(String path, NodeListener listener) throws Exception;

    /**
     * 构建zookeeper注册中心
     *
     * @param config zookeeper配置
     * @return zookeeper注册中心
     */
    static Registry buildZKRegistry(ZKConfig config) {
        return new ZKRegistry(config);
    }
}
