package com.joe.easysocket.client.ext;

import com.joe.easysocket.client.data.Datagram;
import com.joe.easysocket.client.data.InterfaceData;

/**
 * @author joe
 * @version 2018.05.08 14:51
 */
public interface MessageListener extends EventListenerAdapter {
    @Override
    default void receive(Datagram data) {
        if (data.getType() == 0) {
            //心跳包
            return;
        }

        InterfaceData interfaceData = getSerializer().read(data.getBody(), InterfaceData.class);
        receive(interfaceData);
    }

    /**
     * 收到数据报回调
     *
     * @param data 收到的数据
     */
    void receive(InterfaceData data);

    /**
     * 数据报解析器
     *
     * @return 解析器
     */
    Serializer getSerializer();
}
