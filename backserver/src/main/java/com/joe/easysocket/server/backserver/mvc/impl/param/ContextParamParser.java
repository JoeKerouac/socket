package com.joe.easysocket.server.backserver.mvc.impl.param;

import java.lang.annotation.Annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.context.Session;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;
import com.joe.utils.reflect.BaseType;

/**
 * Context参数解析器
 *
 * @author joe
 */
@Provider
public class ContextParamParser implements ParamInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ContextParamParser.class);

    @Override
    public boolean isReadable(Param param, String data) {
        Annotation[] annotations = param.getType().getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Context) {
                BaseType type = (BaseType) param.getType();
                if (HttpRequestContext.RequestWrapper.class.isAssignableFrom(type.getType())
                    || Session.class.isAssignableFrom(type.getType())) {
                    return true;
                } else {
                    logger.warn("参数含有@Context注解，但是参数类型是{}", type);
                }
            }
        }
        return false;
    }

    @Override
    public Object read(Param param, HttpRequestContext.RequestWrapper request, String data) {
        try {
            logger.debug("要解析的参数格式为：{}，request为：{}，data为：{}", param, request, data);
            BaseType type = (BaseType) param.getType();
            if (HttpRequestContext.RequestWrapper.class.isAssignableFrom(type.getType())) {
                logger.debug("参数要注入Request");
                return request;
            } else if (Session.class.isAssignableFrom(type.getType())) {
                logger.debug("参数要注入Session");
                return request.getSession();
            } else {
                logger.warn("Context参数解析器发生了不可预知的错误：注入未知类型的参数{}", type.getType());
                return null;
            }
        } catch (Exception e) {
            logger.debug("解析参数{}时出错，参数对应的数据为：{}", param, data, e);
            return null;
        }
    }
}
