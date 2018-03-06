package com.joe.easysocket.server.backserver.mvc.impl.coder.json;

import com.joe.easysocket.server.backserver.mvc.coder.ReaderInterceptor;
import com.joe.easysocket.server.backserver.mvc.coder.WriterInterceptor;
import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.param.Context;
import com.joe.easysocket.server.backserver.mvc.impl.param.ParamParserContainer;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;
import com.joe.utils.parse.json.JsonParser;
import com.joe.utils.type.JavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * json解析器
 *
 * @author joe
 */
@Provider
public class JsonDataInterceptor implements ReaderInterceptor, WriterInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(JsonDataInterceptor.class);
    private static final JsonParser parser = JsonParser.getInstance();
    private ParamParserContainer paramParserContainer;

    @Override
    public boolean isWriteable(String contentType) {
        logger.debug("要写的数据格式为：{}", contentType);
        return "json".equalsIgnoreCase(contentType);
    }

    @Override
    public String write(Object data) {
        logger.debug("要解析的数据为：{}", data);
        String result = parser.toJson(data);
        logger.debug("解析后的数据为：{}", result);
        return result;
    }

    @Override
    public boolean isReadable(String contentType) {
        return "json".equalsIgnoreCase(contentType);
    }

    @Override
    public Object[] read(List<Param> params, HttpRequestContext requestContext, String data) throws
            ParamParserException {
        logger.debug("要解析的参数格式为：{}", params);
        // 方法没有参数
        if (params.isEmpty()) {
            logger.debug("该方法没有参数");
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
            logger.debug("参数为null");
            Object[] result = new Object[params.size()];
            for (int i = 0; i < params.size(); i++) {
                result[i] = null;
            }
            return result;
        }

        if (params.size() == 1) {
            // 只有一个参数
            logger.debug("该资源只有一个参数");
            Param param = params.get(0);
            JavaType type = params.get(0).getType();
            logger.debug("参数{}的类型为{}", param.getName(), type);
            Object obj = paramParserContainer.parse(param, requestContext.getRequest(), data);
            return new Object[]{obj};
        } else if (params.size() == 2 && hasContext) {
            // 除了注入的context外只有一个参数
            logger.debug("该资源除了context外只有一个参数");
            Param param = params.get(1 - contextIndex);
            JavaType type = params.get(1 - contextIndex).getType();
            logger.debug("参数{}的类型为{}", param.getName(), type);

            Object p1 = paramParserContainer.parse(params.get(0), requestContext.getRequest(), data);
            Object p2 = paramParserContainer.parse(params.get(1), requestContext.getRequest(), data);
            return new Object[]{p1, p2};
        } else {
            // 多个参数
            logger.debug("该资源有多个参数");
            Map<String, String> paramMap = parser.readAsMap(data, HashMap.class, String.class, String.class);
            logger.debug("初次解析的参数集合为：{}", paramMap);
            Object[] objParams = new Object[params.size()];
            // 分别解析多个参数
            for (int i = 0; i < params.size(); i++) {
                // 解析参数
                Param param = params.get(i);
                String paramName = param.getName();
                logger.debug("参数{}的类型为{}", paramName, param.getType());
                String paramData = paramMap.get(paramName);
                logger.debug("参数对应的数据为：{}", paramData);

                objParams[i] = this.paramParserContainer.parse(param, requestContext.getRequest(), paramData);
            }
            return objParams;
        }
    }

    @Override
    public void init(ParamParserContainer container) {
        this.paramParserContainer = container;
    }

    /**
     * 将指定byte数据按照指定的字符集转换为字符串（当指定字符集不可用时尝试采用系统默认字符集转换）
     *
     * @param data    指定byte数据
     * @param charset 指定字符集
     * @return 解析后的字符串
     */
    private String convert(byte[] data, String charset) {
        String dataStr = null;
        if (data == null) {
            logger.debug("data为null");
            return null;
        }
        try {
            // 尝试使用用户字符集编码
            dataStr = new String(data, charset);
        } catch (Exception e) {
            // 用户字符集有问题，尝试使用系统默认字符集编码
            logger.error("请求字符集为{}，字符集出错，尝试使用系统默认字符集解析", charset, e);
            dataStr = new String(data);
        }
        return dataStr;
    }

}
