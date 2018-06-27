package com.joe.easysocket.server.common.config;

import com.joe.easysocket.server.common.exception.ConfigIllegalException;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.easysocket.server.common.spi.Registry;
import com.joe.easysocket.server.common.spi.Serializer;
import com.joe.easysocket.server.common.spi.SpiLoader;
import com.joe.easysocket.server.common.spi.impl.serializer.JsonSerializer;
import com.joe.utils.collection.CollectionUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.joe.easysocket.server.common.config.Const.PUBLISH_CENTER;
import static com.joe.easysocket.server.common.config.Const.REGISTRY;

/**
 * @author joe
 * @version 2018.06.27 13:33
 */
@Slf4j
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

    /**
     * 从BaseConfig构建系统环境信息
     *
     * @param config BaseConfig
     * @return 系统环境信息
     */
    public static Environment build(BaseConfig config) {
        ClusterConfig clusterConfig = config.getClusterConfig();
        Environment environment = new Environment(config.getEnvironment());

        Registry registry = environment.getSafe(REGISTRY, Registry.class);
        if (registry == null) {
            String registryClass = clusterConfig.getRegistry();
            log.info("加载spi[{}]", registryClass);
            registry = SpiLoader.loadSpi(registryClass, config.getEnvironment());
            log.info("spi [{}] 加载完毕", registryClass);
        } else {
            log.info("从环境信息中获取到了registry [{}]", registry);
        }

        PublishCenter publishCenter = environment.getSafe(PUBLISH_CENTER, PublishCenter.class);
        if (publishCenter == null) {
            String publishCenterClass = clusterConfig.getPublishCenter();
            log.info("加载spi[{}]", publishCenterClass);
            publishCenter = SpiLoader.loadSpi(publishCenterClass, config.getEnvironment());
            log.info("spi [{}] 加载完毕", publishCenterClass);
        } else {
            log.info("从环境信息中获取到了publishCenter [{}]", publishCenter);
        }

        List<Serializer> serializers;
        List<String> serializerClassList = clusterConfig.getSerializers();

        if (!CollectionUtil.safeIsEmpty(serializerClassList)) {
            serializers = new ArrayList<>(serializerClassList.size() + 1);
            serializerClassList.parallelStream().forEach(serializerClass -> {
                try {
                    log.info("加载spi[{}]", serializerClass);
                    Serializer serializer = SpiLoader.loadSpi(serializerClass, config.getEnvironment());
                    log.info("spi [{}] 加载完毕", serializerClass);
                    serializers.add(serializer);
                } catch (ConfigIllegalException e) {
                    log.warn("序列化器[{}]加载失败，忽略序列化器[{}]", serializerClass, serializerClass, e);
                }
            });
        } else {
            serializers = new ArrayList<>(1);
        }
        log.info("添加JSON序列化器");
        serializers.add(SpiLoader.loadSpi(JsonSerializer.class.getName(), config.getEnvironment()));
        log.info("JSON序列化器添加完毕");

        environment.put(Const.CONFIG, config);
        environment.put(Const.CLUSTER_CONFIG, config.getClusterConfig());
        environment.put(Const.REGISTRY, registry);
        environment.put(Const.PUBLISH_CENTER, publishCenter);
        environment.put(Const.SERIALIZER_LIST, serializers);
        return environment;
    }
}
