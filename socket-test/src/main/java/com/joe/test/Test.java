package com.joe.test;


/**
 * 不依赖于外部系统自启动（spring等）
 *
 * @author joe
 */
public class Test {

    public static void main(String[] args) throws Exception {
        new Thread(Starter::startBackserver, "backserver").start();
        new Thread(Starter::startBalance, "balance").start();
    }
}
