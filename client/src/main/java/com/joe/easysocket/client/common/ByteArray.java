package com.joe.easysocket.client.common;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * byte数组
 * 
 * 
 * @author joe
 *
 */
public class ByteArray implements Iterable<Byte> {
    private volatile int modCount = 0;
    private byte[]       datas    = null;
    private int          size     = 0;

    public ByteArray() {
        this(256);
    }

    /**
     * 构建数组
     * 
     * @param initial
     *            数组初始化数据
     */
    public ByteArray(byte[] initial) {
        this.datas = new byte[initial.length * 3 / 2];
        this.size = 0;
        this.append(initial);
    }

    /**
     * 构建数组
     * 
     * @param initial
     *            数组初始化大小
     */
    public ByteArray(int initial) {
        this.datas = new byte[initial];
        this.size = 0;
    }

    /**
     * 向byte数组中添加数据，包含start，不包含end（运行时会产生end - start大小的额外空间占用）
     * 
     * @param data
     *            数据源
     * @param start
     *            起始位置（包含该位置）
     * @param len
     *            长度
     */
    public void append(byte[] data, int start, int len) {
        if (len <= 0) {
            return;
        }
        byte[] newData = new byte[len];
        System.arraycopy(data, 0, newData, 0, len);
        append(newData);
    }

    /**
     * 向byte数组中添加数据
     * 
     * @param data
     *            添加的数据，不能为null
     */
    public void append(byte[] data) {
        if (data.length >= (this.datas.length - size)) {
            // 扩容
            byte[] newData = new byte[this.datas.length + data.length + this.datas.length / 2];
            System.arraycopy(this.datas, 0, newData, 0, size);
            this.datas = newData;
        }
        System.arraycopy(data, 0, this.datas, size, data.length);
        this.size += data.length;
        modCount++;
    }

    /**
     * 向byte数组中添加数据
     * 
     * @param data
     *            要添加的数据
     */
    public void append(byte data) {
        // 当数组长度等于当前数据大小时进行扩展
        if (0 >= (this.datas.length - size)) {
            // 如果扩展后的数组大于当前数组，那么先把当前数组长度增加，一次扩展256
            byte[] newData = new byte[256 + size];
            System.arraycopy(this.datas, 0, newData, 0, size);
            this.datas = newData;
        }
        this.datas[size] = data;
        this.size += 1;
        modCount++;
    }

    /**
     * 获取数据（获取的备份数据，不是原址）
     * 
     * @return ByteArray中的数据
     */
    public byte[] getData() {
        byte[] data = new byte[size];
        System.arraycopy(this.datas, 0, data, 0, size);
        return data;
    }

    /**
     * 获取数组大小（实际数据大小）
     * 
     * @return 数组大小
     */
    public int size() {
        return this.size;
    }

    /**
     * 获取指定位置的数据
     * 
     * @param index
     *            位置
     * @return 指定位置的数据
     * @throws IndexOutOfBoundsException
     *             当index大于长度时抛出
     */
    public byte get(int index) throws IndexOutOfBoundsException {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("数组大小为：" + this.size + ";下标为：" + index);
        }
        return datas[index];
    }

    /**
     * 将指定位置的数据替换为指定数据
     * 
     * @param data
     *            指定数据
     * @param index
     *            指定位置
     * @throws IndexOutOfBoundsException
     *             数组越界时抛出该异常
     */
    public void replace(byte data, int index) throws IndexOutOfBoundsException {
        if (index >= this.size || index < 0) {
            throw new IndexOutOfBoundsException("数组大小为：" + this.size + ";下标为：" + index);
        }
        this.datas[index] = data;
    }

    @Override
    public Iterator<Byte> iterator() {
        return new ByteArrayIterator(this);
    }

    private class ByteArrayIterator implements Iterator<Byte> {
        int                     expectedModCount = modCount;
        private final ByteArray byteArray;
        private int             point            = 0;

        ByteArrayIterator(final ByteArray byteArray) {
            this.byteArray = byteArray;
        }

        @Override
        public boolean hasNext() {
            return byteArray.size > point;
        }

        @Override
        public Byte next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("没有更多数据了");
            }
            checkForComodification();
            return byteArray.get(point++);
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
}
