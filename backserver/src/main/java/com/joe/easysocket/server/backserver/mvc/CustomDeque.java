package com.joe.easysocket.server.backserver.mvc;


import com.joe.easysocket.server.common.lambda.Serializer;

import java.util.concurrent.BlockingDeque;

/**
 * 双向阻塞队列，序列化器默认使用json序列化器
 *
 * @author joe
 */
public interface CustomDeque<E> extends BlockingDeque<E> {
    /**
     * 注册序列化器
     *
     * @param serializer 要注册的序列化器
     */
    void register(Serializer serializer);
}
