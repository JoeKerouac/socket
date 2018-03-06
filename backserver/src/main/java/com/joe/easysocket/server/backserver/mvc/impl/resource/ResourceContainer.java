package com.joe.easysocket.server.backserver.mvc.impl.resource;


import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractContainer;
import com.joe.easysocket.server.backserver.mvc.container.Selector;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 资源容器，该类的实例应该为全局唯一
 *
 * @author joe
 */
public class ResourceContainer extends AbstractContainer<Resource> {
    public ResourceContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }

    // 构建API
    private ApiBuilder apiBuilder = ApiBuilder.getInstance();
    // 资源容器，key为资源路径，value为对应的资源
    private Map<String, Resource> container;

    @Override
    public List<Resource> select(Selector<Resource> selector) {
        // 目前这种写法效率略低，每次都要遍历，推荐使用findResource
        for (Map.Entry<String, Resource> entry : container.entrySet()) {
            Resource resource = entry.getValue();
            if (selector.select(resource)) {
                return Collections.singletonList(resource);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 根据资源名查找资源
     *
     * @param name 资源名
     * @return 对应的资源，如果资源不存在那么返回null
     */
    public Resource findResource(String name) {
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        return container.get(name);
    }

    @Override
    public void start() {
        // 资源容器不能调用父类的初始化方法
        logger.debug("开始初始化资源");
        // 构建资源
        Map<String, Object> map = beanContainer.getBeansWithAnnotation(Path.class);
        if (!map.isEmpty()) {
            List<Object> list = new ArrayList<>(map.size());
            map.forEach((key, value) -> list.add(value));
            this.container = apiBuilder.buildResourceFromObject(list);
        } else {
            this.container = Collections.emptyMap();
            logger.warn("没有找到资源");
        }
        logger.debug("资源初始化完毕");
        return;
    }

    @Override
    public void shutdown() {
        logger.debug("开始销毁资源容器");
        if (this.container != null) {
            container.clear();
        }
        container = null;
        apiBuilder = null;
        logger.debug("资源容器销毁完毕");
    }

    @Override
    public void register(String name, Resource component) {
        container.put(name, component);
    }

    @Override
    public void register(Map<String, Resource> components) {
        container.putAll(components);
    }

    @Override
    public void register(Class<Resource> clazz, Resource resource) {
        container.put(resource.getName(), resource);
    }
}
