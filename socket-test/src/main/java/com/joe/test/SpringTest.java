package com.joe.test;

import com.joe.easysocket.server.backserver.mvc.impl.container.CustomClassPathBeanDefinitionScanner;
import com.joe.easysocket.server.backserver.mvc.impl.container.SpringBeanContainer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 集成spring，使用SpringBeanContainer的示例
 *
 * @author joe
 * @version 2018.06.25 18:13
 */
public class SpringTest {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        //扫描自定义的bean
        CustomClassPathBeanDefinitionScanner scanner = new CustomClassPathBeanDefinitionScanner(context);
        scanner.scan("com");
        context.refresh();
        new Thread(() -> Starter.startBackserver(new SpringBeanContainer(context)), "backserver").start();
        new Thread(Starter::startBalance, "balance").start();
    }
}
