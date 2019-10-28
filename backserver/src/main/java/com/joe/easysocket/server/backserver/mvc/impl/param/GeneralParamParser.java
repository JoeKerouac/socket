package com.joe.easysocket.server.backserver.mvc.impl.param;

import java.lang.annotation.Annotation;

import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Consumes;
import com.joe.utils.reflect.type.JavaType;
import com.joe.utils.reflect.type.JavaTypeUtil;
import com.joe.utils.serialize.json.JsonParser;

import lombok.extern.slf4j.Slf4j;

/**
 * 普通参数解析器（只能解析json类型的输入参数）
 *
 * @author joe
 */
@Slf4j
@Provider
public class GeneralParamParser implements ParamInterceptor {
    private static final JsonParser JSON_PARSER = JsonParser.getInstance();

    @Override
    public boolean isReadable(Param param, String data) {
        Consumes consumes = param.getMethod().getAnnotation(Consumes.class);
        if (consumes != null && !"json".equals(consumes.value())) {
            log.debug("参数[{}]所在方法参数解析不是json类型", param);
            return false;
        }
        Annotation[] annotations = param.getType().getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof GeneralParam) {
                log.debug("参数[{}]带有@GeneralParam注解并且资源方法声明的参数解析器是json，使用GeneralParamParser处理",
                    param);
                return true;
            }
        }
        log.debug("参数[{}]没有@GeneralParam注解", param);
        return false;
    }

    @Override
    public Object read(Param param, HttpRequestContext.RequestWrapper request,
                       String data) throws ParamParserException {
        JavaType type = param.getType();
        log.debug("将{}解析为{};参数{}的类型为{}", data, type, param.getName(), type);
        try {
            Object result = JSON_PARSER.readAsObject(data, JavaTypeUtil.getRealType(type));
            log.debug("读取出来的参数是：{}", result);
            return result;
        } catch (Throwable e) {
            log.error("解析参数{}时出错，用户数据为：{}", param, data, e);
            throw new ParamParserException(param.getName(), e.toString());
        }
    }
}
