package com.joe.easysocket.server.common.lambda;

import java.util.concurrent.locks.Lock;

/**
 * 函数接口
 *
 * @author joe
 */
@FunctionalInterface
public interface Function {
    /**
     * 要执行的函数
     */
    void exec();

    /**
     * 根据条件加锁并执行函数
     *
     * @param condition 条件，如果条件为true则执行函数
     * @param lock      执行过程中加锁，加锁后再次判断条件是否满足
     * @return 如果函数被成功执行那么返回true
     */
    default boolean lockAndExec(Condition condition, Object lock) {
        if (condition.enable()) {
            synchronized (lock) {
                if (condition.enable()) {
                    this.exec();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据条件加锁并执行函数
     *
     * @param condition 执行条件，条件满足时执行函数
     * @param lock      执行过程中加锁，加锁后再次判断条件是否满足
     * @return 函数是否执行，返回true表示函数执行
     */
    default boolean lockAndExec(Condition condition, Lock lock) {
        if (condition.enable()) {
            lock.lock();
            try {
                if (condition.enable()) {
                    this.exec();
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * 根据条件加锁并执行指定函数
     *
     * @param function  要执行的函数
     * @param condition 执行条件，条件为true时执行函数
     * @param lock      执行过程中加锁，加锁后再次判断条件是否满足
     * @return 如果函数被成功执行那么返回true
     */
    static boolean lockAndExec(Function function, Condition condition, Object lock) {
        return function.lockAndExec(condition, lock);
    }

    /**
     * 根据条件加锁并执行指定函数
     *
     * @param function  要执行的函数
     * @param condition 执行条件，条件为true时执行函数
     * @param lock      执行过程中加锁，加锁后再次判断条件是否满足
     * @return 如果函数被成功执行那么返回true
     */
    static boolean lockAndExec(Function function, Condition condition, Lock lock) {
        return function.lockAndExec(condition, lock);
    }
}
