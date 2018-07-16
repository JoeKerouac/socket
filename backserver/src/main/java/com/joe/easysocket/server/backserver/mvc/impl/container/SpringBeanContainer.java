package com.joe.easysocket.server.backserver.mvc.impl.container;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.common.exception.SystemException;

/**
 * 如果使用该BeanContainer使用组件{@link CustomClassPathBeanDefinitionScanner CustomClassPathBeanDefinitionScanner}
 * 扫描（基于注解扫描，如果是基于xml只需要自己将组件注册即可）
 *
 * @author joe
 * @version 2018.06.25 10:44
 */
public class SpringBeanContainer implements BeanContainer {
    private ApplicationContext context;

    public SpringBeanContainer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        return context.getBeansWithAnnotation(annotationType);
    }

    @Override
    public ClassLoader getClassLoader() {
        return context.getClassLoader();
    }

    @Override
    public void start() throws SystemException {
        //不需要start
    }

    @Override
    public void shutdown() throws SystemException {
        //不需要shutdown
    }
}
