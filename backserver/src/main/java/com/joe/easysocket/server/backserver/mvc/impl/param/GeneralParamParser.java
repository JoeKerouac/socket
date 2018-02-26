package com.joe.easysocket.server.backserver.mvc.impl.param;

import com.joe.easysocket.server.backserver.mvc.impl.container.Provider;
import com.joe.easysocket.server.backserver.mvc.impl.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;
import com.joe.utils.parse.json.JsonParser;
import com.joe.utils.type.JavaType;
import com.joe.utils.type.JavaTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * 普通参数解析器
 *
 * @author joe
 */
@Provider
public class GeneralParamParser implements ParamInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ParamInterceptor.class);
    private static final JsonParser parser = JsonParser.getInstance();

    @Override
    public boolean isReadable(Param param, String data) {
        Annotation[] annotations = param.getType().getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof GeneralParam) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object read(Param param, RequestContext.RequestWrapper request, String data) throws ParamParserException {
        JavaType type = param.getType();
        logger.debug("将{}解析为{};参数{}的类型为{}", data, type, param.getName(), type);
        try {
            Object result = parser.readAsObject(data, JavaTypeUtil.getRealType(type));
            logger.debug("读取出来的参数是：{}", result);
            return result;
        } catch (Throwable e) {
            logger.error("解析参数{}时出错，用户数据为：{}", param, data, e);
            throw new ParamParserException(param.getName(), e.toString());
        }
    }
}
