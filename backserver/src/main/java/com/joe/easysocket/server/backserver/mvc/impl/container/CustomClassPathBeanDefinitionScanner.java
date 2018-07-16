package com.joe.easysocket.server.backserver.mvc.impl.container;

import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * 如果使用{@link SpringBeanContainer SpringBeanContainer}，那么需要将该类注册到spring中
 *
 * @author joe
 * @version 2018.06.25 10:53
 */
public class CustomClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public CustomClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    public void registerDefaultFilters() {
        // 添加自定义类型，将以下注解加入扫描
        ScanConfig.SOCKET_COMPONENT.parallelStream().map(AnnotationTypeFilter::new)
            .forEach(this::addIncludeFilter);
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        return super.doScan(basePackages);
    }

    @Override
    public boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return super.isCandidateComponent(beanDefinition);
    }
}
