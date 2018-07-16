package com.joe.easysocket.server.backserver.mvc.impl.param;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 参数命名注解
 *
 * @author joe
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface GeneralParam {
    /**
     * 参数名
     *
     * @return 参数名
     */
    String value() default "";
}
