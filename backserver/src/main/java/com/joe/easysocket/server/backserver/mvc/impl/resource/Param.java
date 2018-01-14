package com.joe.easysocket.server.backserver.mvc.impl.resource;

import com.joe.utils.type.JavaType;
import lombok.Data;

/**
 * API参数
 * 
 * @author joe
 *
 * @param <T>
 */
@Data
public class Param<T> {
	/*
	 * 参数名
	 */
	private String name;
	/*
	 * 参数类型
	 */
	private JavaType type;
}