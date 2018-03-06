package com.joe.easysocket.server.backserver.mvc.impl.container;

import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.utils.concurrent.LockService;
import com.joe.utils.scan.ClassScanner;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 提供最基础的实现，如果bean的类不包含无参数构造器那么该bean将不会被扫描到
 * <p>
 * 该类主要用于测试，不建议正式使用
 *
 * @author joe
 */
public class BeanContainerImpl implements BeanContainer {
    private static final Logger logger = LoggerFactory.getLogger(BeanContainerImpl.class);
    private Map<Class<? extends Annotation>, Map<String, Object>> beanCache;
    private List<Class<?>> allBeanClass;
    private String[] args;
    private boolean init = false;

    /**
     * 默认构造器，参数传需要扫描的包（可以只传入跟目录，然后会自动递归）
     *
     * @param args 要扫描的包
     */
    public BeanContainerImpl(@NonNull String... args) {
        this.args = args;
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        logger.debug("查找所有注解为{}的bean", annotationType);
        if (annotationType == null) {
            return Collections.emptyMap();
        }

        if (beanCache.containsKey(annotationType)) {
            logger.debug("在缓存中找到注解为{}的bean集合", annotationType);
            return beanCache.get(annotationType);
        }

        try {
            LockService.lock(annotationType.getSimpleName());
            if (beanCache.containsKey(annotationType)) {
                logger.debug("在缓存中找到注解为{}的bean集合", annotationType);
                return beanCache.get(annotationType);
            }
            logger.debug("缓存中不存在指定注解的bean集合，开始生成");

            List<Object> allObj = allBeanClass.stream().filter(clazz -> clazz.getDeclaredAnnotation(annotationType)
                    != null).map(clazz -> {
                try {
                    //调用默认无参数构造
                    Class<?>[] empty = {};
                    //获取构造器，然后将构造器的accessible设置为true，防止因为构造器私有而造成反射调用失败
                    Constructor constructor = clazz.getDeclaredConstructor(empty);
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                } catch (Exception e) {
                    logger.debug("生成{}的实例时失败，忽略该类，该类的注解为：{}", clazz, annotationType, e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            logger.debug("当前所有带有注解{}的类的实例为：{}", annotationType, allObj);

            if (allObj.isEmpty()) {
                beanCache.put(annotationType, Collections.emptyMap());
                return Collections.emptyMap();
            }

            Map<String, Object> result = new HashMap<>();
            allObj.forEach(obj -> result.put(obj.getClass().getName(), obj));
            logger.debug("当前所有带有注解{}的类的实例集合为：{}", annotationType, result);
            beanCache.put(annotationType, result);
            return result;
        } finally {
            LockService.unlock(annotationType.getSimpleName());
        }
    }

    @Override
    public synchronized void start() {
        logger.info("准备初始化默认bean容器");
        if (init) {
            logger.warn("bean容器已经初始化，请勿重复初始化");
            return;
        }
        ClassScanner scanner = ClassScanner.getInstance();
        allBeanClass = scanner.scan((Object[]) args);
        beanCache = new HashMap<>();
        init = true;
        logger.debug("Bean容器初始化完毕");
    }

    @Override
    public synchronized void shutdown() {
        logger.info("准备销毁默认bean容器");
        if (!init) {
            logger.warn("bean容器未初始化或已经销毁");
            return;
        }
        beanCache.clear();
        allBeanClass.clear();
        init = false;
        logger.info("bean容器销毁完毕");
    }
}