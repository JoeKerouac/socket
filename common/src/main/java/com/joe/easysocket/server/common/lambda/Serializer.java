package com.joe.easysocket.server.common.lambda;

import com.joe.easysocket.server.common.exception.SerializeException;
import com.joe.utils.parse.json.JsonParser;

/**
 * 序列化器
 *
 * @author joe
 */
public interface Serializer {
    /**
     * 序列化
     *
     * @param obj 要序列化的数据
     * @return 序列化结果，序列化失败时返回null
     * @throws SerializeException 序列化异常
     */
    byte[] write(Object obj) throws SerializeException;

    /**
     * 反序列化
     *
     * @param data  要反序列化的数据
     * @param clazz 类型
     * @param <T>   数据类型
     * @return 序列化后的对象，反序列化失败时返回null
     * @throws SerializeException 序列化异常
     */
    <T> T read(byte[] data, Class<T> clazz) throws SerializeException;

    /**
     * 是否可写
     *
     * @param obj 要序列化的对象
     * @return 返回true表示可写
     */
    boolean writeable(Object obj);

    /**
     * 是否可读
     *
     * @param clazz 要反序列化的对象
     * @param <T>   对象类型
     * @return 返回true表示可读
     */
    <T> boolean readable(Class<T> clazz);

    /**
     * 默认序列化器
     */
    Serializer DEFAULTSER = new Serializer() {
        private final JsonParser parser = JsonParser.getInstance();

        @Override
        public byte[] write(Object obj) {
            if (obj == null) {
                return null;
            }
            return parser.toJson(obj).getBytes();
        }

        @Override
        public <T> T read(byte[] data, Class<T> clazz) {
            if (data == null || clazz == null) {
                return null;
            }
            return parser.readAsObject(data, clazz);
        }

        @Override
        public boolean writeable(Object obj) {
            return true;
        }

        @Override
        public <T> boolean readable(Class<T> clazz) {
            return true;
        }
    };
}
