package com.joe.easysocket.server.backserver.mvc.impl.resource.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * resource类的注解
 *
 * @author joe
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface Path {
    /**
     * 路径
     *
     * @return 路径
     */
    String value();
}
