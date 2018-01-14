package com.joe.easysocket.client.data;

import lombok.Data;

/**
 * 接口调用消息，对应数据报（Datagram）的body
 *
 * @author joe
 */
@Data
public class InterfaceData implements com.joe.easysocket.client.data.Data {
    // 该消息ID，需要确保短时间内是唯一的（客户端生成，响应的时候将该ID返回去）
    private String id;
    // 调用的接口，例如/say
    private String invoke;
    // 要发送的数据
    private String data;

    public InterfaceData() {
        // 必须保留该构造，json序列化使用
    }

    /**
     * @param id        消息ID，需要确保短时间内唯一（客户端生成，响应的时候将该ID返回去）
     * @param invoke    调用的接口
     * @param data      要发送的数据
     */
    public InterfaceData(String id, String invoke, String data) {
        this.id = id;
        this.invoke = invoke;
        this.data = data;
    }
}
