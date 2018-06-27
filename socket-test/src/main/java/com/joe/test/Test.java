package com.joe.test;


/**
 * 不依赖于外部系统自启动（spring等）
 *
 * @author joe
 */
public class Test {

    public static void main(String[] args) throws Exception {
        /*
         * 如果要使用redis作为PUB/SUB中心，使用zookeeper作为注册中心的话使用Starter.useNet();代替下边的一行
         */
        Starter.useLocal();
        new Thread(Starter::startBackserver, "backserver").start();
        new Thread(Starter::startBalance, "balance").start();
    }
}
