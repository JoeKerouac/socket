package com.joe.easysocket.server.common.config;

import lombok.NonNull;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author joe
 * @version 2018.06.27 13:33
 */
public final class Environment {
    private ConcurrentHashMap<Object, Object> environment;

    public Environment() {
        this(null);
    }

    public Environment(Properties environment) {
        this.environment = new ConcurrentHashMap<>();
        put(environment);
    }

    /**
     * 将properties放入环境
     *
     * @param properties properties
     */
    public void put(Properties properties) {
        if (properties != null) {
            environment.putAll(properties);
        }
    }

    /**
     * 放入环境
     *
     * @param key   key，不能为null
     * @param value value，不能为null
     */
    public void put(@NonNull Object key, @NonNull Object value) {
        environment.put(key, value);
    }

    /**
     * 从环境取出指定key对应的value
     *
     * @param key key，不能为null
     * @param <T> value类型
     * @return value
     */
    public <T> T get(@NonNull Object key) {
        return (T) environment.get(key);
    }

    /**
     * 从环境安全的取出指定key对应的value
     *
     * @param key   key
     * @param clazz value的Class，不能为空
     * @param <T>   value类型
     * @return value，如果指定key对应的值为null或者不是指定类型则返回null
     */
    public <T> T getSafe(@NonNull Object key, @NonNull Class<T> clazz) {
        Object obj = environment.get(key);
        if (obj != null && clazz.isAssignableFrom(obj.getClass())) {
            return (T) obj;
        } else {
            return null;
        }
    }
}
