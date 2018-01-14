package com.joe.easysocket.server.backserver.mvc.impl.resource;

import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path;
import com.joe.utils.scan.MethodFilter;

import java.lang.reflect.Method;


/**
 * resource方法过滤器
 *
 * @author joe
 */
public class ResourceMethodFilter implements MethodFilter {
    public boolean filter(Method method) {
        return method.isAnnotationPresent(Path.class);
    }
}
