package com.joe.easysocket.server.backserver.mvc.impl.container;

import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.common.exception.SystemException;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 如果使用该BeanContainer需要注册组件{@link CustomClassPathBeanDefinitionScanner CustomClassPathBeanDefinitionScanner}
 * 到spring
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
