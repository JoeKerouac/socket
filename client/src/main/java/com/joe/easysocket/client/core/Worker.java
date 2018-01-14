package com.joe.easysocket.client.core;

import com.joe.easysocket.client.common.DatagramUtil;
import com.joe.easysocket.client.ext.Logger;
import com.joe.easysocket.client.ext.Serializer;

/**
 * 工作器
 *
 * @author joe
 */
public abstract class Worker {
    //序列化器
    Serializer serializer;
    //日志对象
    Logger logger;
    //当前服务器状态
    volatile boolean shutdown = true;
    //工作线程
    Thread worker;
    //关闭回调，系统关闭时会调用
    Callback callback;
    //数据报工具
    DatagramUtil datagramUtil;

    public Worker(Logger logger, Callback callback, Serializer serializer) {
        this.logger = logger;
        this.callback = callback;
        this.serializer = serializer;
        this.datagramUtil = new DatagramUtil(logger);
    }

    /**
     * 关闭 读取/发送 线程
     *
     * @return 返回true表示关闭成功，返回false表示当前已经关闭（重复关闭）
     */
    public synchronized boolean shutdown() {
        if (isShutdown()) {
            return false;
        }
        logger.debug("读取/发送 线程关闭");
        //必须先将shutdown置为true，work线程的正常中断依赖于该变量
        shutdown = true;
        worker.interrupt();
        callback.exec();
        return true;
    }

    /**
     * 获取当前工作器是否关闭
     *
     * @return 返回true表示已经关闭或者未启动，返回false表示未关闭
     */
    public boolean isShutdown() {
        return shutdown;
    }
}
