package com.joe.easysocket.server.backserver.mvc.impl.filter;


import com.joe.easysocket.server.backserver.mvc.impl.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractSpringContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.Provider;
import com.joe.easysocket.server.backserver.mvc.impl.container.Selector;
import com.joe.easysocket.server.backserver.mvc.impl.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.ResponseContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.FilterException;
import com.joe.utils.scan.ScannerException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * filter容器
 *
 * @author joe
 */
public class FilterContainer extends AbstractSpringContainer<NioFilter> {
    public FilterContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }

    // 存放filter，其中key为优先级
    private TreeMap<Integer, NioFilter> filters;
    private boolean init = false;

    public void register(List<Class<NioFilter>> classes) {
        classes.forEach(clazz -> {
            try {
                register((Class<NioFilter>) null, clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("filter初始化失败", e);
                throw new ScannerException("filter初始化失败", e);
            }
        });
    }

    @Override
    public void register(Class<NioFilter> clazz, NioFilter filter) {
        try {
            register((String) null, filter);
        } catch (Exception e) {
            logger.debug("注册filter时异常", e);
            throw new ScannerException("注册filter时异常", e);
        }
    }

    @Override
    public void register(String name, NioFilter component) {
        logger.debug("注册filter：{}", component);
        Provider provider = component.getClass().getAnnotation(Provider.class);
        // 优先级
        int priority = provider.priority();
        if (priority <= 0) {
            priority = 1;
        }
        priority *= 10000;
        // 加上随机数，主要为了对同优先级的filter进行排序，同时随机数的最大值不能超过10000，否则会影响filter本身的优先级设置
        int random = (int) (Math.random() * 5000);
        // 保证容器中该优先级是唯一的
        while (filters.containsKey(priority + random)) {
            random = (int) (Math.random() * 5000);
        }
        priority += random;
        logger.debug("注册filter：{}，注册完毕，优先级为：{}", component, priority);

        filters.put(priority, component);
    }

    @Override
    public void register(Map<String, NioFilter> components) {
        components.forEach(this::register);
    }

    @Override
    public synchronized void start() {
        logger.info("开始初始化filter容器");
        if (init) {
            logger.warn("filter容器已经初始化过，不能重复初始化");
        }
        init = true;
        //必须先初始化
        //该行必须放在super.init()上方，否则会空指针异常
        //降序排列
        filters = new TreeMap<>((arg0, arg1) -> arg1 - arg0);
        super.start();

        logger.debug("filter容器初始化完毕");
    }

    @Override
    public synchronized void shutdown() {
        logger.info("开始销毁filter容器");
        if (!init) {
            logger.warn("filter容器已经销毁或者未初始化，不能销毁");
        }
        init = false;
        super.shutdown();
        filters.clear();
        logger.debug("filter容器销毁完毕");
    }

    @Override
    public List<NioFilter> select(Selector<NioFilter> selector) {
        // 该容器不支持
        logger.warn("该容器不支持select方法");
        return Collections.emptyList();
    }

    /**
     * 请求filter
     *
     * @param request 请求信息
     */
    public void requestFilter(RequestContext.RequestWrapper request) throws FilterException {
        try {
            logger.debug("开始请求filter");
            filters.entrySet().forEach(entry -> entry.getValue().requestFilter(request));
            logger.debug("filter结束");
        } catch (Throwable e) {
            logger.error("请求filter过程中发生了异常");
            throw new FilterException(e);
        }
    }

    /**
     * 响应filter
     *
     * @param request  请求信息
     * @param response 响应信息
     */
    public void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response) throws
            FilterException {
        try {
            logger.debug("开始响应filter");
            filters.entrySet().forEach(entry -> entry.getValue().responseFilter(request, response));
            logger.debug("响应filter结束");
        } catch (Throwable e) {
            logger.error("响应filter过程中发生了异常");
            throw new FilterException(e);
        }
    }
}
