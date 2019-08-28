package com.joe.easysocket.server.backserver.mvc.impl.resource;

import java.lang.reflect.Method;
import java.util.List;

import javax.validation.ValidationException;

import com.joe.easysocket.server.backserver.mvc.Bean;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ResourceInvokeException;
import com.joe.utils.reflect.ReflectUtil;
import com.joe.utils.validation.ValidatorUtil;

import lombok.Data;

/**
 * 资源接口
 *
 * @author joe
 */
@Data
public class Resource<T> implements Bean {
    // 该资源对应的实体类
    private final Class<T>    resourceClass;
    // 该资源对应的方法
    private final Method      resourceMethod;
    // 该资源的访问路径，格式是/aa/bb，以/开头
    private final String      name;
    // 访问该资源需要的参数
    private final List<Param> params;
    private final T           instance;
    // 响应数据格式
    private final String      produce;
    // 接收数据格式
    private final String      consume;

    public Resource(Class<T> resourceClass, Method resourceMethod, String name, List<Param> params,
                    T instance, String produce, String consume) {
        this.resourceClass = resourceClass;
        this.resourceMethod = resourceMethod;
        this.name = name;
        this.params = params;
        this.instance = instance;
        this.produce = produce;
        this.consume = consume;
        ReflectUtil.allowAccess(this.resourceMethod);
    }

    /**
     * 调用资源
     *
     * @param requestContext 请求上下文
     * @return 调用结果
     * @throws ResourceInvokeException 资源调用异常
     */
    public Object invoke(HttpRequestContext requestContext) throws ResourceInvokeException {
        try {
            return ReflectUtil.invoke(instance, resourceMethod, requestContext.getParams());
        } catch (Throwable e) {
            throw new ResourceInvokeException(e);
        }
    }

    /**
     * 检查参数是否符合注解（用户自行加的验证注解）
     *
     * @param params 参数
     * @throws ValidationException 当参数不符合规定时抛出该异常
     */
    public void check(Object[] params) throws ValidationException {
        ValidatorUtil.validateParameters(instance, resourceMethod, params);
    }
}
