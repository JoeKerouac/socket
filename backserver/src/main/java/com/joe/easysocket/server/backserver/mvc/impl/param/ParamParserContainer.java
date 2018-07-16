package com.joe.easysocket.server.backserver.mvc.impl.param;

import java.util.List;

import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.container.AbstractContainer;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;

/**
 * 参数解析器的容器，存放参数解析器（带有注解@ParamParser并且实现ParamInterceptor接口）
 *
 * @author joe
 */
public class ParamParserContainer extends AbstractContainer<ParamInterceptor> {

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
    public Object parse(Param param, HttpRequestContext.RequestWrapper request,
                        String data) throws ParamParserException {
        logger.debug("开始解析参数{}", param);
        List<ParamInterceptor> paramInterceptors = select(t -> t.isReadable(param, data));
        if (paramInterceptors.isEmpty()) {
            logger.warn("没有找到指定参数{}对应的解析器", param);
            return null;
        }
        return paramInterceptors.get(0).read(param, request, data);
    }
}
