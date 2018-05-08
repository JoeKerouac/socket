package com.joe.easysocket.client.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简单的LRU实现（如果map的size超过初始化时指定的大小，那么将会清除最少使用或者最早放入的。
 * 注：首先根据使用次数清除，使用次数相同的根据放入顺序清除）
 *
 * @param <K> map中key的泛型
 * @param <V> map中value的泛型
 * @author joe
 */
public class LRUCacheMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = -1576619020580779936L;
    private int max;

    /**
     * 从其他map构建一个LRUmap
     *
     * @param m 现有Map，会将该Map转换为LRUMap
     */
    LRUCacheMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    /**
     * LRU Map 构造器
     *
     * @param initialCapacity 初始大小
     * @param loadFactor      负载因子
     * @param accessOrder     the ordering mode - true for access-order, false for insertion-order
     */
    LRUCacheMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }

    /**
     * 默认构造器
     */
    public LRUCacheMap() {
        this(1000);
    }

    /**
     * 带初始化大小的构造器
     *
     * @param initialCapacity 初始化大小
     */
    public LRUCacheMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * 带初始化大小和负载因子的构造器
     *
     * @param initialCapacity 初始化大小
     * @param loadFactor      负载因子
     */
    public LRUCacheMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
        this.max = initialCapacity;
    }

    /**
     * 是否删除map中最后的元素
     *
     * @param eldest 最后一个元素
     * @return 返回true表示删除多余的元素
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > this.max;
    }
}
