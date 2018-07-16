package com.joe.easysocket.server.backserver.mvc.impl.resource;

import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path;
import com.joe.utils.scan.ClassFilter;

/**
 * resource类过滤器
 * @author joe
 *
 */
public class ResourceClassFilter implements ClassFilter {

    public boolean filter(Class<?> clazz) {
        return clazz.isAnnotationPresent(Path.class);
    }
}
