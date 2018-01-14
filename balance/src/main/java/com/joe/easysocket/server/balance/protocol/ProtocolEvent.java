package com.joe.easysocket.server.balance.protocol;

/**
 * 协议栈事件（其中RECEIVED是在协议栈接收到下层消息时触发，SEND是在应用层传往协议栈的消息发送成功后触发）
 *
 * @author joe
 */
public enum ProtocolEvent {
    // 协议栈从下层接收到消息后发布该事件
    RECEIVED("协议栈从下层收到消息"),
    // 消息处理完毕后发布该事件
    RECEIVEDSUCCESS("协议栈从下层收到消息并且应用层成功处理并返回"),
    // 协议栈从下层收到消息但是处理失败时发布该事件
    RECEIVEDERROR("协议栈从下层收到消息但是处理失败"),
    // 消息发送后发布该事件（底层可能未发出，但是从协议栈发出了）
    SEND("协议栈发出消息"),
    // 注册成功后发布该事件
    REGISTER("通道注册成功"),
    // 通道注销后发布该事件，该事件会传入一个String类型的通道ID和一个CloseCause类型的关闭原因
    UNREGISTER("通道注销"),
    // 通道注销但是有未读完的信息，该事件会传入String类型的通道ID和byte[]类型的数据（该数据不是一个完整的数据
    // 报，如果有需要可以自己解析）
    DISCARD("通道注销但是有未读完的信息");

    private String value;

    ProtocolEvent(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }

}
