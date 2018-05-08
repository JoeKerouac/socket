package com.joe.easysocket.client.ext;


import com.joe.easysocket.client.Client;
import com.joe.easysocket.client.core.EventListener;
import com.joe.easysocket.client.core.SocketEvent;
import com.joe.easysocket.client.data.Datagram;
import com.joe.easysocket.client.data.InterfaceData;

/**
 * 事件适配器
 *
 * @author joe
 */
public interface EventListenerAdapter extends EventListener {
    default void listen(SocketEvent event, Object... args) {
        switch (event) {
            case FAILD:
                faild((Throwable) args[0]);
                break;
            case REGISTER:
                register((Client) args[0]);
                break;
            case RECONNECT:
                reconnect((Client) args[0]);
                break;
            case UNREGISTER:
                unregister();
                break;
            case RECEIVE:
                receive((Datagram) args[0]);
                break;
            default:
                throw new RuntimeException("没有监听[" + event + "]事件");
        }
    }

    /**
     * 连接失败，并且自动重连失败
     *
     * @param cause 失败原因
     */
    default void faild(Throwable cause){}

    /**
     * 连接注册成功
     *
     * @param client 注册成功后的client
     */
    default void register(Client client){}

    /**
     * 自动重连成功（由于底层只是自动建立连接，并没有重新登录等逻辑，所以需要用户自己实现重连后的逻辑，例如重新登录）
     *
     * @param client 重连后的client
     */
    default void reconnect(Client client){}

    /**
     * 通道关闭（用户主动调用shutdown）
     */
    default void unregister(){}

    /**
     * 收到数据报回调
     *
     * @param data 收到的数据
     */
    default void receive(Datagram data){}
}
