package com.joe.easysocket.client.ext;

/**
 * 序列化器
 *
 * @author joe
 */
public interface Serializer {
    /**
     * 序列化对象
     *
     * @param obj 要序列化的对象，不能为null
     * @return 序列化后的数据
     */
    byte[] write(Object obj);

    /**
     * 反序列化对象
     *
     * @param data  对象数据，不能为null
     * @param clazz 对象的Class，不能为null
     * @param <T>   对象类型
     * @return 反序列化得到的对象
     */
    <T> T read(byte[] data, Class<T> clazz);
}
