package com.joe.easysocket.server.common.spi;

import lombok.NonNull;

import java.util.Properties;

/**
 * SPI，所有的spi接口都应该继承该接口
 *
 * @author joe
 * @version 2018.06.27 11:17
 */
public interface Spi {
    /**
     * 配置SPI，使用SPI实现类前会先调用该方法
     *
     * @param environment 环境
     */
    void setProperties(@NonNull Properties environment);
}
