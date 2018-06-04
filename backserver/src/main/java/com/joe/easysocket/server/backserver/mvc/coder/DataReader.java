package com.joe.easysocket.server.backserver.mvc.coder;


import com.joe.easysocket.server.backserver.mvc.container.Provider;
import com.joe.easysocket.server.backserver.mvc.Bean;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.param.ParamParserContainer;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;

import java.util.List;

/**
 * 数据解析器接口，必须实现该接口并且注解{@link Provider}才会生效
 *
 * @author joe
 */
public interface DataReader extends Bean {
    /**
     * 是否可读
     *
     * @param contentType 接口的参数格式
     * @return true：可读;false：不可读
     */
    boolean isReadable(String contentType);

    /**
     * 将数据读取为接口需要的参数数组
     *
     * @param params         调用的接口参数说明
     * @param requestContext 请求上下文
     * @param data           接口调用数据
     * @return 读取的参数数组
     * @throws ParamParserException 数据读取成功但是校验失败
     */
    Object[] read(List<Param> params, HttpRequestContext requestContext, String data) throws ParamParserException;

    /**
     * 初始化
     *
     * @param container 提供一个参数容器，提供解析不同注解类型的参数的能力
     */
    void init(ParamParserContainer container);
}
