package com.joe.easysocket.server.backserver.mvc.impl.resource.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 定义响应数据的处理方式
 *
 * @author joe
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD })
public @interface Produces {
    /**
     * 响应数据的处理方式，默认json
     *
     * @return 响应数据的处理方式
     */
    String value() default "json";
}
