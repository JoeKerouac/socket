package com.joe.easysocket.server.backserver.mvc.impl.param;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 通过该注解注入request
 * 
 * @author joe
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface Context {

}
