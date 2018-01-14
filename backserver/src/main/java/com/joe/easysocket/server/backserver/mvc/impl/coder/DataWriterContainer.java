package com.joe.easysocket.server.backserver.mvc.impl.coder;


import com.joe.easysocket.server.backserver.mvc.impl.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractSpringContainer;

/**
 * 响应数据处理器容器
 *
 * @author joe
 */
public class DataWriterContainer extends AbstractSpringContainer<WriterInterceptor> {
    public DataWriterContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }
}
