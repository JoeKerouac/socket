package com.joe.easysocket.server.common.lambda;

/**
 * 条件
 *
 * @author joe
 */
@FunctionalInterface
public interface Condition {
    /**
     * 条件是否通过
     *
     * @return 返回true表示允许，返回false表示不允许
     */
    boolean enable();
}
