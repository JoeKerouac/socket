package com.joe.easysocket.server.backserver.mvc.impl.param;

import com.joe.easysocket.server.backserver.mvc.Bean;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Param;

/**
 * 参数解析器
 *
 * @author joe
 */
public interface ParamInterceptor extends Bean {
    /**
     * 该参数是否可以解析
     *
     * @param param 参数说明
     * @param data  参数数据
     * @return true：可读;false：不可读
     */
    boolean isReadable(Param param, String data);

    /**
     * 将数据读取为需要的参数，该方法不能抛出异常
     *
     * @param param   参数说明
     * @param request request对象
     * @param data    参数数据
     * @return 读取的参数，发生异常时返回null
     */
    Object read(Param param, HttpRequestContext.RequestWrapper request,
                String data) throws ParamParserException;
}
