package com.joe.easysocket.server.backserver.mvc.impl.param;

import com.joe.easysocket.server.backserver.mvc.impl.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractSpringContainer;
import com.joe.easysocket.server.backserver.mvc.impl.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;

import java.util.List;

/**
 * 参数解析器的容器，存放参数解析器（带有注解@ParamParser并且实现ParamInterceptor接口）
 *
 * @author joe
 */
public class ParamParserContainer extends AbstractSpringContainer<ParamInterceptor> {

    public ParamParserContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }

    /**
     * 解析参数
     *
     * @param param   要解析的参数说明
     * @param request request对象
     * @param data    请求数据
     * @return 解析后的参数
     * @throws ParamParserException 参数解析完成但是校验失败
     */
    public Object parse(Param param, RequestContext.RequestWrapper request, String data) throws
            ParamParserException {
        logger.debug("开始解析参数{}", param);
        List<ParamInterceptor> paramInterceptors = select(t -> t.isReadable(param, data));
        if (paramInterceptors.isEmpty()) {
            logger.warn("没有找到指定参数{}对应的解析器", param);
            return null;
        }
        return paramInterceptors.get(0).read(param, request, data);
    }
}
