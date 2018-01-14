package com.joe.easysocket.client.exception;

/**
 * 数据报过大
 *
 * @author joe
 */
public class DataOutOfMemory extends SystemException {
    private static final long serialVersionUID = -6207632501397751811L;

    public DataOutOfMemory(String message) {
        super(message);
    }
}
