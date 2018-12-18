package com.joe.easysocket.server.backserver.mvc.impl.coder.json;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.joe.easysocket.server.backserver.mvc.coder.DataReader;
import com.joe.easysocket.server.backserver.mvc.coder.DataWriter;
import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.param.Context;
import com.joe.easysocket.server.backserver.mvc.impl.param.ParamParserContainer;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;
import com.joe.utils.reflect.JavaType;
import com.joe.utils.serialize.json.JsonParser;

import lombok.extern.slf4j.Slf4j;

/**
 * json解析器
 *
 * @author joe
 */
@Slf4j
@Provider
public class JsonDataRW implements DataReader, DataWriter {
    private static final JsonParser JSON_PARSER = JsonParser.getInstance();
    private ParamParserContainer    paramParserContainer;

    @Override
    public boolean isWriteable(String contentType) {
        log.debug("要写的数据格式为：{}", contentType);
        return "json".equalsIgnoreCase(contentType);
    }

    @Override
    public String write(Object data) {
        log.debug("要解析的数据为：{}", data);
        String result = JSON_PARSER.toJson(data);
        log.debug("解析后的数据为：{}", result);
        return result;
    }

    @Override
    public boolean isReadable(String contentType) {
        return "json".equalsIgnoreCase(contentType);
    }

    @Override
    public Object[] read(List<Param> params, HttpRequestContext requestContext,
                         String data) throws ParamParserException {
        log.debug("要解析的参数格式为：{}", params);
        // 方法没有参数
        if (params.isEmpty()) {
            log.debug("该方法没有参数");
            return null;
        }

        // 检查参数中是否有注入的context元素
        boolean hasContext = false;
        int contextIndex = 0;
        for (; contextIndex < params.size(); contextIndex++) {
            Param param = params.get(contextIndex);
            Annotation[] annotations = param.getType().getAnnotations();
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (annotation != null && annotation instanceof Context) {
                        hasContext = true;
                        break;
                    }
                }
                if (hasContext) {
                    break;
                }
            }
        }

        if (!hasContext && data == null) {
            log.debug("参数为null");
            Object[] result = new Object[params.size()];
            for (int i = 0; i < params.size(); i++) {
                result[i] = null;
            }
            return result;
        }

        if (params.size() == 1) {
            // 只有一个参数
            log.debug("该资源只有一个参数");
            Param param = params.get(0);
            JavaType type = params.get(0).getType();
            log.debug("参数{}的类型为{}", param.getName(), type);
            Object obj = paramParserContainer.parse(param, requestContext.getRequest(), data);
            return new Object[] { obj };
        } else if (params.size() == 2 && hasContext) {
            // 除了注入的context外只有一个参数
            log.debug("该资源除了context外只有一个参数");
            Param param = params.get(1 - contextIndex);
            JavaType type = params.get(1 - contextIndex).getType();
            log.debug("参数{}的类型为{}", param.getName(), type);

            Object p1 = paramParserContainer.parse(params.get(0), requestContext.getRequest(),
                data);
            Object p2 = paramParserContainer.parse(params.get(1), requestContext.getRequest(),
                data);
            return new Object[] { p1, p2 };
        } else {
            // 多个参数
            log.debug("该资源有多个参数");
            Map<String, String> paramMap = JSON_PARSER.readAsMap(data, HashMap.class, String.class,
                String.class);
            log.debug("初次解析的参数集合为：{}", paramMap);
            Object[] objParams = new Object[params.size()];
            // 分别解析多个参数
            for (int i = 0; i < params.size(); i++) {
                // 解析参数
                Param param = params.get(i);
                String paramName = param.getName();
                log.debug("参数{}的类型为{}", paramName, param.getType());
                String paramData = paramMap.get(paramName);
                log.debug("参数对应的数据为：{}", paramData);

                objParams[i] = this.paramParserContainer.parse(param, requestContext.getRequest(),
                    paramData);
            }
            return objParams;
        }
    }

    @Override
    public void init(ParamParserContainer container) {
        this.paramParserContainer = container;
    }
}
