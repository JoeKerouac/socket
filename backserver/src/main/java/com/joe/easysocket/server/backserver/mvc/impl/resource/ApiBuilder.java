package com.joe.easysocket.server.backserver.mvc.impl.resource;

import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path;
import com.joe.utils.scan.ClassScanner;
import com.joe.utils.scan.MethodScanner;
import com.joe.utils.scan.ScannerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * API构建工具
 *
 * @author joe
 */
class ApiBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ApiBuilder.class);
    private static final Object lock = new Object();
    private ClassScanner classScanner = ClassScanner.getInstance();
    private MethodScanner methodScanner = MethodScanner.getInstance();
    private ResourceClassFilter resourceClassFilter = new ResourceClassFilter();
    private ResourceMethodFilter resourceMethodFilter = new ResourceMethodFilter();
    private static volatile ApiBuilder apiBuilder;

    private ApiBuilder() {
    }

    public static ApiBuilder getInstance() {
        if (apiBuilder == null) {
            synchronized (lock) {
                if (apiBuilder == null) {
                    apiBuilder = new ApiBuilder();
                }
            }
        }
        return apiBuilder;
    }

    /**
     * 使用指定的类构建资源
     *
     * @param arg 指定的对象
     * @return 该类构建的资源，可能返回空集合，不会返回null
     */
    public Map<String, Resource> buildResourceFromObject(Object arg) {
        logger.debug("使用类{}构建资源", arg);
        Class<?> clazz = arg.getClass();
        if (!clazz.isAnnotationPresent(Path.class)) {
            return Collections.emptyMap();
        }
        // 扫描API类中的API方法
        List<Method> methodList = methodScanner.scan(Collections.singletonList(resourceMethodFilter), clazz);
        if (methodList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Resource> resources = new TreeMap<>();
        // 构建API说明并添加到apiList中去
        resources.putAll(ApiUtil.buildResource(arg, clazz, methodList.toArray(new Method[methodList.size()])));
        return resources;
    }

    /**
     * 使用指定的类构建资源
     *
     * @param clazz 指定的类
     * @return 该类构建的资源，可能返回空集合，不会返回null
     */
    public Map<String, Resource> buildResourceFromClass(Class<?> clazz) {
        logger.debug("使用类{}构建资源", clazz);
        if (!clazz.isAnnotationPresent(Path.class)) {
            return Collections.emptyMap();
        }
        // 构建API说明
        try {
            return buildResourceFromObject(clazz.newInstance());
        } catch (Exception e) {
            throw new ScannerException("构建API对象时实例化失败", e);
        }
    }

    /**
     * 扫描所有资源类（带@Path注解的类）中的资源方法（带@Path注解的方法）
     *
     * @param root 要扫描的根路径
     * @return key为资源路径，value为该路径对应的资源的map
     */
    public Map<String, Resource> buildResource(String root) {
        logger.debug("开始扫描资源，扫描根路径为：{}", root);
        // 扫描所有API类
        List<Class<?>> classList = classScanner.scan(Collections.singletonList(resourceClassFilter), root);
        return buildResourceFromClass(classList);
    }

    /**
     * 扫描所有资源类（带@Path注解的类）中的资源方法（带@Path注解的方法）
     *
     * @param classList 要扫描的所有类
     * @return key为资源路径，value为该路径对应的资源的map
     */
    public Map<String, Resource> buildResourceFromClass(Collection<Class<?>> classList) {
        logger.debug("开始扫描资源，扫描根路径为：{}", classList);
        if (classList.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Resource> resources = new TreeMap<>();
        classList.forEach(clazz -> resources.putAll(buildResourceFromClass(clazz)));
        logger.debug("对类集合{}扫描完毕", classList);
        return resources;
    }

    /**
     * 根据资源对象构建资源说明
     *
     * @param args 资源对象的集合
     * @return 资源说明
     */
    public Map<String, Resource> buildResourceFromObject(Collection<Object> args) {
        logger.debug("开始扫描资源，扫描根路径为：{}", args);
        if (args.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Resource> resources = new TreeMap<>();
        args.forEach(clazz -> resources.putAll(buildResourceFromObject(clazz)));
        logger.debug("对类集合{}扫描完毕", args);
        return resources;
    }
}
