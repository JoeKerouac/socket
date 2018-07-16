package com.joe.easysocket.server.backserver.mvc;

import java.util.function.Consumer;

import com.joe.easysocket.server.backserver.mvc.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.data.InterfaceData;
import com.joe.easysocket.server.common.lambda.Endpoint;
import com.joe.utils.protocol.Datagram;

/**
 * MVC控制器
 *
 * @author joe
 * @version 2018.03.06 14:43
 */
public interface MvcController extends Endpoint {
    /**
     * 处理数据
     *
     * @param datagram       待处理数据
     * @param requestContext RequestContext的实例，需要外部注入
     * @param consumer       数据回调，当MVC控制器处理完数据后会调用该回调，回调数据为处理结果
     * @param <R>            RequestContext的实际类型
     * @throws NullPointerException 当任意一个参数为空时抛出该异常
     */
    <R extends RequestContext> void deal(Datagram datagram, R requestContext,
                                         Consumer<InterfaceData> consumer) throws NullPointerException;
}
