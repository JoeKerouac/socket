package com.joe.easysocket.server.backserver.mvc.coder;


import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractContainer;

/**
 * 响应数据处理器容器
 *
 * @author joe
 */
public class DataWriterContainer extends AbstractContainer<DataWriter> {
    public DataWriterContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }
}
