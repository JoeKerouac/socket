package com.joe.easysocket.server.backserver.mvc.impl.exceptionmapper;


import com.joe.easysocket.server.backserver.mvc.impl.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractSpringContainer;

/**
 * 异常处理器容器
 * 
 * @author joe
 *
 */
public class ExceptionMapperContainer extends AbstractSpringContainer<ExceptionMapper> {
    public ExceptionMapperContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }
}
