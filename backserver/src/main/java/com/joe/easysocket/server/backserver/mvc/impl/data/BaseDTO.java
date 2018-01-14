package com.joe.easysocket.server.backserver.mvc.impl.data;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * 基本DTO，所有DTO父类
 * 
 * @author Administrator
 *
 */
@Data
public class BaseDTO<T> implements Serializable {
	private static final long serialVersionUID = 5075924626508128661L;
	private static final Map<String, String> msg = new TreeMap<String, String>();
	static {
		// 成功
		msg.put("200", "Success");
		// 参数错误
		msg.put("400", "Fail Param");
		// 找不到参数解析器
		msg.put("505", "Data Parser Not Found");
		// 找不到指定资源
		msg.put("404", "NotFoundResource");
		// 系统错误
		msg.put("500", "systemError");
		// 数据非法
		msg.put("501", "DataError");
		// 已经有其他用户登录
		msg.put("502", "Other User Login");
		// 密码修改，请重新登录
		msg.put("503", "Password Has Changed");
		// 心跳超时
		msg.put("504", "Timeout");
		// 参数验证失败
		msg.put("505", "validation fail");
	}
	/**
	 * 请求状态
	 */
	private String status;
	/**
	 * 错误消息
	 */
	private String message;
	/**
	 * 数据
	 */
	private T data;

	public BaseDTO() {
		this.success();
	}

	public BaseDTO(String status, String message) {
		this.status(status, message);
	}

	public BaseDTO(String status) {
		this.setStatus(status);
	}

	/**
	 * 设置状态码
	 * 
	 * @param status
	 *            状态码
	 * @param message
	 *            错误消息
	 */
	public void status(String status, String message) {
		this.status = status;
		this.message = message;
	}

	public void status(String status) {
		this.message = msg.get(status);
		this.status = status;
	}

	/**
	 * 系统错误，未知原因（异常）
	 * 
	 * @param status
	 */
	public void systemError() {
		this.setStatus("500");
	}

	/**
	 * 请求成功
	 */
	public void success() {
		setStatus("200");
	}

	/**
	 * 构建简单的成功对象
	 * 
	 * @return 成功DTO
	 */
	public static <T> BaseDTO<T> buildSuccess() {
		return new BaseDTO<T>();
	}

	/**
	 * 构建指定结果的DTO（状态为200）
	 * 
	 * @param t
	 *            结果
	 * @return 包含指定结果的DTO
	 */
	public static <T> BaseDTO<T> build(T t) {
		BaseDTO<T> dto = new BaseDTO<T>();
		dto.setData(t);
		return dto;
	}

	/**
	 * 系统错误
	 * 
	 * @return 系统错误DTO
	 */
	public static <T> BaseDTO<T> buildError() {
		BaseDTO<T> dto = new BaseDTO<T>();
		dto.systemError();
		return dto;
	}

	/**
	 * 指定预设错误
	 * 
	 * @param status
	 *            错误状态码
	 * @return 错误DTO
	 */
	public static <T> BaseDTO<T> buildError(String status) {
		return new BaseDTO<T>(status);
	}

	/**
	 * 构建指定错误
	 * 
	 * @param status
	 *            错误状态码
	 * @param message
	 *            错误说明
	 * @return 错误消息
	 */
	public static <T> BaseDTO<T> buildError(String status, String message) {
		return new BaseDTO<T>(status, message);
	}
}
