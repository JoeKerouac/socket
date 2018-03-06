package com.joe.easysocket.server.backserver.mvc.container;

/**
 * 选择器
 *
 * @author joe
 */
public interface Selector<T> {
    /**
     * 选择器
     *
     * @param t 要选择的组件
     * @return 如果传入的组件是需要的组件，那么返回true，否则返回false
     */
    boolean select(T t);
}
