package com.joe.easysocket.server.backserver.mvc.impl.coder;


import com.joe.easysocket.server.backserver.mvc.impl.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractSpringContainer;
import com.joe.easysocket.server.backserver.mvc.impl.param.ParamParserContainer;

import java.util.List;

/**
 * 请求数据处理器容器
 *
 * @author joe
 */
public class DataReaderContainer extends AbstractSpringContainer<ReaderInterceptor> {
    private ParamParserContainer paramParserContainer;

    public DataReaderContainer(BeanContainer beanContainer, ParamParserContainer paramParserContainer) {
        super(beanContainer);
        this.paramParserContainer = paramParserContainer;
    }

    @Override
    public void initBean(List<ReaderInterceptor> beans) {
        beans.forEach(bean -> bean.init(paramParserContainer));
    }
}
