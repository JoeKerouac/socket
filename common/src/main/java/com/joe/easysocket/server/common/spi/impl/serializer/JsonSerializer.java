package com.joe.easysocket.server.common.spi.impl.serializer;


import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.spi.Serializer;
import com.joe.utils.serialize.json.JsonParser;

/**
 * JSON序列化器
 *
 * @author joe
 * @version 2018.06.27 15:19
 */
public class JsonSerializer implements Serializer {
    private static final JsonParser JSON_PARSER = JsonParser.getInstance();

    @Override
    public byte[] write(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON_PARSER.write(obj);
    }

    @Override
    public <T> T read(byte[] data, Class<T> clazz) {
        if (data == null || clazz == null) {
            return null;
        }
        return JSON_PARSER.read(data, clazz);
    }

    @Override
    public boolean writeable(Object obj) {
        return true;
    }

    @Override
    public <T> boolean readable(Class<T> clazz) {
        return true;
    }

    @Override
    public void setProperties(Environment environment) {

    }
}
