package com.joe.easysocket.server.backserver.mvc;


import com.joe.easysocket.server.common.lambda.Serializer;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * 双向阻塞队列的默认实现，不提供分布式实现，只有单机功能，需要分布式部署请自行实现CustomDeque接口
 *
 * @author joe
 */
public class CustomDequeImpl<E> extends LinkedBlockingDeque<E> implements CustomDeque<E> {
    @Override
    public void register(Serializer serializer) {
        //单机版不存在序列化问题，所以无需序列化器
    }
}
