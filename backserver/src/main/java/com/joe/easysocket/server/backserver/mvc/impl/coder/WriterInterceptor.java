package com.joe.easysocket.server.backserver.mvc.impl.coder;


import com.joe.easysocket.server.backserver.mvc.impl.Bean;

/**
 * 响应数据解析器接口，必须实现该接口并且注解{@link com.joe.easysocket.server.backserver.mvc.impl.container.Provider}才会生效
 *
 * @author joe
 */
public interface WriterInterceptor extends Bean {
    /**
     * 是否可写
     *
     * @param contentType 接口返回的数据格式
     * @return <li>true：可写</li>
     * <li>false：不可写</li>
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
