package com.joe.test;

import com.joe.utils.validation.ValidatorUtil;

import javax.validation.constraints.Size;

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

//        ValidatorUtil.validate(new Pojo());
    }


    public static class Pojo{
        @Size(min = 0, max = 5)
        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
