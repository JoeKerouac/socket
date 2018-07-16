package com.joe.easysocket.server.backserver.mvc.impl.exceptionmapper;

import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractContainer;

/**
 * 异常处理器容器
 *
 * @author joe
 */
public class ExceptionMapperContainer extends AbstractContainer<ExceptionMapper> {
    public ExceptionMapperContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }
}
