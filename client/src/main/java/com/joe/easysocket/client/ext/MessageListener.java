package com.joe.easysocket.client.ext;

import com.joe.easysocket.client.common.LRUCacheMap;
import com.joe.easysocket.client.data.Datagram;
import com.joe.easysocket.client.data.InterfaceData;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author joe
 * @version 2018.05.08 14:51
 */
public abstract class MessageListener implements EventListenerAdapter {
    private static final int CACHE_SIZE = 500;
    private static final int CACHE_MAX_SIZE = 5000;
    private LRUCacheMap<String, String> cache;
    /**
     * 用来辅助cache做并发控制
     */
    private final ConcurrentHashMap<String, String> lock;

    public MessageListener() {
        this.cache = new LRUCacheMap<>(CACHE_SIZE);
        this.lock = new ConcurrentHashMap<>();
    }

    @Override
    public void receive(Datagram data) {
        if (data.getType() == 0) {
            //心跳包
            return;
        }
        String id = new String(data.getId());
        String value = lock.putIfAbsent(id, id);
        //如果value等于null表示该ID是第一次出现，如果不是说明数据重复，不进行处理
        if (value == null) {
            cache.put(id, id);
            InterfaceData interfaceData = getSerializer().read(data.getBody(), InterfaceData.class);
            receive(interfaceData);
        }
        if (lock.size() >= CACHE_MAX_SIZE) {
            synchronized (lock) {
                if (lock.size() >= CACHE_MAX_SIZE) {
                    lock.clear();
                    lock.putAll(cache);
                }
            }
        }
    }

    /**
     * 收到数据报回调
     *
     * @param data 收到的数据
     */
    public abstract void receive(InterfaceData data);

    /**
     * 数据报解析器
     *
     * @return 解析器
     */
    public abstract Serializer getSerializer();
}
