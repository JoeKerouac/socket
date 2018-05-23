package com.joe.easysocket.server.backserver.mvc.coder;


import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractContainer;
import com.joe.easysocket.server.backserver.mvc.impl.param.ParamParserContainer;

import java.util.List;

/**
 * 请求数据处理器容器
 *
 * @author joe
 */
public class DataReaderContainer extends AbstractContainer<DataReader> {
    private ParamParserContainer paramParserContainer;

    public DataReaderContainer(BeanContainer beanContainer, ParamParserContainer paramParserContainer) {
        super(beanContainer);
        this.paramParserContainer = paramParserContainer;
    }

    @Override
    public void initBean(List<DataReader> beans) {
        beans.forEach(bean -> bean.init(paramParserContainer));
    }
}
