package com.joe.easysocket.server.backserver.mvc.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口调用消息，对应数据报（Datagram）的body
 *
 * @author joe
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceData implements com.joe.easysocket.server.common.data.Data {
    //该消息ID，需要确保短时间内是唯一的（客户端生成，响应的时候将该ID返回去）
    //暂时没有必要使用，datagram中有该字段
    private String id;
    // 调用的接口，例如/say
    private String invoke;
    // 要发送的数据
    private String data;
}
