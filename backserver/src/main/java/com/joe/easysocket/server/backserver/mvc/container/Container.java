package com.joe.easysocket.server.backserver.mvc.container;


import com.joe.easysocket.server.backserver.mvc.Bean;
import com.joe.easysocket.server.common.lambda.Endpoint;

import java.util.List;

/**
 * 容器
 *
 * @param <T> 容器中放置的组件的类型
 * @author joe
 */
public interface Container<T extends Bean> extends Register<T>, Endpoint {
    /**
     * 根据名称选择组件
     *
     * @param selector 组件选择器，返回所有符合条件的组件
     * @return 指定组件，如果没有返回null
     */
    List<T> select(Selector<T> selector);

    /**
     * 初始化bean，在容器中的bean被找出后根据需要选择性的初始化
     *
     * @param beans 所有的bean
     */
    void initBean(List<T> beans);
}
