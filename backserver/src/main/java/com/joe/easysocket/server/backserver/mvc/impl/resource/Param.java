package com.joe.easysocket.server.backserver.mvc.impl.resource;

import java.lang.reflect.Method;

import com.joe.utils.reflect.type.JavaType;

import lombok.Data;

/**
 * API参数
 *
 * @author joe
 */
@Data
public class Param {
    /**
     * 参数名
     */
    private String   name;
    /**
     * 参数类型
     */
    private JavaType type;
    /**
     * 参数所在方法
     */
    private Method   method;
}