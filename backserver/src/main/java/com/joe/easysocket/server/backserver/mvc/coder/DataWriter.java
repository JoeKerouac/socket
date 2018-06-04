package com.joe.easysocket.server.backserver.mvc.coder;


import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.Bean;

/**
 * 响应数据解析器接口，必须实现该接口并且注解{@link Provider}才会生效
 *
 * @author joe
 */
public interface DataWriter extends Bean {
    /**
     * 是否可写
     *
     * @param contentType 接口返回的数据格式
     * @return true：可写；false：不可写
     */
    boolean isWriteable(String contentType);

    /**
     * 将数据序列化
     *
     * @param data 需要序列化的数据
     * @return 序列化后的数据
     */
    String write(Object data);
}
