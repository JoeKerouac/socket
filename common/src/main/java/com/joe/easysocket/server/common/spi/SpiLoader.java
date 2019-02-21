package com.joe.easysocket.server.common.spi;

import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.exception.ConfigIllegalException;
import com.joe.utils.common.StringUtils;
import com.joe.utils.reflect.ClassUtils;
import com.joe.utils.reflect.ReflectException;

import lombok.extern.slf4j.Slf4j;

/**
 * 加载SPI
 *
 * @author joe
 * @version 2018.06.27 11:24
 */
@Slf4j
public class SpiLoader {

    /**
     * 加载Spi实现类的class
     * @param className spi实现类的名称
     * @param parent 对应的spi
     * @param <T> spi类型
     * @return spi实现类的class对象
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> loadSpiClass(String className, Class<T> parent) {
        Class<?> clazz;
        try {
            clazz = ClassUtils.loadClass(className);
        } catch (ReflectException e) {
            log.error("指定的Spi[{}]不存在");
            throw new ConfigIllegalException(StringUtils.format("指定的Spi[{0}]不存在", className), e);
        }
        if (!parent.isAssignableFrom(clazz)) {
            throw new ConfigIllegalException(
                StringUtils.format("指定的Spi[{0}]不是[{1}]的子类", className, parent.getName()));
        }
        return (Class<T>) clazz;
    }

    /**
     * 加载SPI
     *
     * @param spiClass    spi对应的class
     * @param environment 系统环境
     * @param <T>         SPI实际类型
     * @return spi实例
     */
    public static <T extends Spi> T loadSpi(String spiClass, Class<T> parent,
                                            Environment environment) {
        log.debug("初始化SPI[{}]", spiClass);
        Class<T> clazz = loadSpiClass(spiClass, parent);
        try {
            T t = ClassUtils.getInstance(clazz);
            t.init(environment);
            log.debug("[{}]初始化完毕", spiClass);
            return t;
        } catch (Throwable e) {
            log.error("构造实例[{}]失败，可能是没有无参数构造器，请为类[{}]增加无参数构造器", spiClass, spiClass, e);
            throw new ConfigIllegalException(
                StringUtils.format("构造实例[{0}]失败，可能是没有无参数构造器，请为类[{1}]增加无参数构造器", spiClass, spiClass),
                e);
        }
    }
}
