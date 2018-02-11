package com.joe.easysocket.server.balance.protocol;

import lombok.Data;

/**
 * 虚拟通道和真实通道间的消息模型
 *
 * @author joe
 */
@Data
public class ChannelData {
    /**
     * 消息类型
     */
    private ChannelDataType type;
    /**
     * 消息
     */
    private byte[] data;

    /**
     * 通道消息类型
     */
    public enum ChannelDataType {
        REGISTER("注册消息"), DATA("数据消息"), ACK("确认消息"), CLOSE("通道关闭");
        private String type;

        ChannelDataType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.type;
        }
    }
}
