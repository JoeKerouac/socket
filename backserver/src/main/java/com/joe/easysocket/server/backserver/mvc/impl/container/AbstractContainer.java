package com.joe.easysocket.server.backserver.mvc.impl.container;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joe.easysocket.server.backserver.mvc.Bean;
import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.container.Container;
import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.container.Selector;
import com.joe.utils.reflect.ClassUtils;

/**
 * 抽象容器，类必须加上注解{@link Provider}才能被该容器发现
 *
 * @param <T> 容器中存放的bean类型
 * @author joe
 */
public abstract class AbstractContainer<T extends Bean> implements Container<T> {
    protected final Logger  logger = LoggerFactory.getLogger(this.getClass());
    /**
     * bean容器
     */
    protected BeanContainer beanContainer;
    /**
     * 容器ClassLoader
     */
    protected ClassLoader   loader;
    /**
     * 当前container容器存放的实体类型
     */
    protected Class<T>      clazz;
    /**
     * container
     */
    protected List<T>       container;
    /**
     * 是否初始化，true表示已经初始化
     */
    private boolean         init;

    @SuppressWarnings("unchecked")
    public AbstractContainer(BeanContainer beanContainer) {
        this.beanContainer = beanContainer;
        this.loader = beanContainer.getClassLoader();
        // 反射获取Container存放的Bean类型
        try {
            String className = getClass().getName();
            logger.debug("当前容器名字为：{}", className);
            String beanClassName = className.replace("Container", "");
            logger.debug("获取的bean类名为：{}", beanClassName);
            this.clazz = ClassUtils.loadClass(beanClassName, loader);
        } catch (Exception e) {
            logger.debug("通过名字获取bean类型失败，尝试通过泛型获取");
            Type genericSuperclass = getClass().getGenericSuperclass();
            logger.debug("类型为：{}", genericSuperclass);
            // 只检查一层Repository泛型参数，不检查父类
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                this.clazz = (Class<T>) actualTypeArguments[0];
            } else {
                throw new RuntimeException("请检查Container类泛型或命名");
            }
        }
    }

    @Override
    public void initBean(List<T> beans) {
        //空实现，各个子容器根据需要对bean进行初始化
    }

    @Override
    public List<T> select(Selector<T> selector) {
        List<T> list = new ArrayList<>();
        container.forEach(t -> {
            if (selector.select(t)) {
                list.add(t);
            }
        });
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void start() {
        if (init) {
            return;
        }
        init = true;
        logger.debug("开始初始化容器");
        this.container = new ArrayList<>();
        Map<String, Object> map = beanContainer.getBeansWithAnnotation(Provider.class);

        map.forEach((key, value) -> {
            if (this.clazz.isAssignableFrom(value.getClass())) {
                logger.debug("注册组件{}", value);
                register((String) null, (T) value);
            }
        });

        initBean(this.container);
        logger.debug("容器初始化完毕");
    }

    @Override
    public synchronized void shutdown() {
        if (!init) {
            return;
        }
        init = false;
        logger.debug("开始销毁容器");
        this.container.clear();
        logger.debug("容器销毁完毕");
    }

    @Override
    public void register(String name, T component) {
        if (component != null) {
            container.add(component);
        }
    }

    @Override
    public void register(Map<String, T> components) {
        components.forEach((key, value) -> register((String) null, value));
    }

    @Override
    public void register(Class<T> clazz, T component) {
        register((String) null, component);
    }
}
