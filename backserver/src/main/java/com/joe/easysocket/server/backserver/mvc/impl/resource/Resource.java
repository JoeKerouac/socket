package com.joe.easysocket.server.backserver.mvc.impl.resource;

import com.joe.easysocket.server.backserver.mvc.Bean;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ResourceInvokeException;
import lombok.Data;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 资源接口
 *
 * @author joe
 */
@Data
public class Resource<T> implements Bean {
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ExecutableValidator executableValidator = validator.forExecutables();
    // 该资源对应的实体类
    private Class<T> resourceClass;
    // 该资源对应的方法
    private Method resourceMethod;
    // 该资源的访问路径，格式是/aa/bb，以/开头
    private String name;
    // 访问该资源需要的参数
    private List<Param> params;
    private T instance;
    // 响应数据格式
    private String produce;
    // 接收数据格式
    private String consume;

    /**
     * 调用资源
     *
     * @param requestContext 请求上线文
     * @return 调用结果
     * @throws ResourceInvokeException 资源调用异常
     */
    public Object invoke(HttpRequestContext requestContext) throws ResourceInvokeException {
        try {
            return resourceMethod.invoke(instance, requestContext.getParams());
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
        Set<ConstraintViolation<T>> methodValidators = executableValidator.validateParameters(instance,
                resourceMethod, params);
        Iterator<ConstraintViolation<T>> iterator = methodValidators.iterator();
        if (iterator.hasNext()) {
            throw new ValidationException(iterator.next().getMessage());
        }
    }
}
