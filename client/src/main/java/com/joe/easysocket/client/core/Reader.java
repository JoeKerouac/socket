package com.joe.easysocket.client.core;


import com.joe.easysocket.client.Client;
import com.joe.easysocket.client.common.DatagramUtil;
import com.joe.easysocket.client.data.Datagram;
import com.joe.easysocket.client.ext.InternalLogger;
import com.joe.easysocket.client.ext.Logger;
import com.joe.easysocket.client.ext.Serializer;
import com.joe.utils.collection.CollectionUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * socket数据读取器
 *
 * @author joe
 */
public class Reader extends Worker {
    // 数据报head中长度字段的起始位置（从0开始）
    private final int lengthFieldOffset = 1;
    // 数据报head的长度
    private final int headLength = Datagram.HEADER;
    //socket输入流
    private final SocketChannel channel;
    //缓冲区大小
    private int bufferSize;
    //线程池
    private final ExecutorService service;
    //客户端
    private Client client;
    //事件中心
    private final EventCenter eventCenter;
    //缓冲区
    private ByteBuffer buffer;

    /**
     * 缓冲区读指针
     */
    private int readPoint = 0;
    /**
     * 缓冲区写指针，相对于缓冲区，往缓冲区中写
     */
    private int writePoint = 0;


    /**
     * 读取器构造器
     *
     * @param channel     socket的输入流
     * @param logger      日志对象
     * @param callback    关闭回调
     * @param serializer  序列化器
     * @param client      client
     * @param eventCenter 事件中心
     */
    public Reader(SocketChannel channel, Logger logger, Callback callback, Serializer serializer, Client client,
                  EventCenter eventCenter) {
        super(logger instanceof InternalLogger ? logger : InternalLogger.getLogger(logger, Reader.class), callback,
                serializer);
        this.channel = channel;
        this.bufferSize = 1024;
        this.service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        this.client = client;
        this.eventCenter = eventCenter;
        this.buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    /**
     * 启动数据读取器
     */
    public synchronized void start() {
        if (!isShutdown()) {
            throw new IllegalThreadStateException("请勿重复启动读取器");
        }
        shutdown = false;
        this.worker = new Thread(() -> {
            logger.info("数据读取器启动");
            try {
                read();
            } catch (IOException e) {
                logger.debug("读取器输入流读取发生异常，中断工作" + e);
            } catch (InterruptedException e) {
                logger.debug("读取器被外部中断，停止工作");
            } finally {
                if (!isShutdown()) {
                    shutdown();
                }
            }
        }, "数据读取器");
        this.worker.start();
    }


    /**
     * 从socket输入流中读取
     *
     * @throws IOException
     */
    private void read() throws IOException, InterruptedException {
        while (!isShutdown()) {
            //重置会写指针
            buffer.position(writePoint);
            //开始从socket读取
            if ((writePoint += channel.read(buffer)) == -1) {
                logger.error("socket输入流被关闭，读取结束");
                shutdown();
            }

            readData(buffer);
        }
    }

    /**
     * 从ByteBuffer中读取数据
     *
     * @param buffer ByteBuffer，当前出入的ByteBuffer的指针应该处于write指针而不是read指针
     */
    private void readData(ByteBuffer buffer) {
        //死循环，一直到缓冲区中数据读完（或者不完整）
        while (true) {
            //获取当前可读取的内容
            final int readable = writePoint - readPoint;

            try {
                //数据报长度，包含请求头
                int dataLen;

                //判断当前有没有读到完整的数据报头（有了数据报的头部就可以分析数据报是否读取完整了）
                if (readable >= headLength) {
                    //切换到读指针
                    buffer.position(readPoint);

                    byte[] header = new byte[headLength];
                    buffer.get(header);
                    //重置指针位置（get操作会更新position）
                    buffer.position(readPoint);
                    dataLen = DatagramUtil.convert(header, lengthFieldOffset) + headLength;
                    logger.debug("读取到数据报的长度，数据报长度为：" + dataLen);
                } else {
                    logger.debug("当前数据不够长，提前终止");
                    break;
                }

                if (readable >= dataLen) {
                    logger.debug("本次数据报数据读取完毕");
                    //完整的数据报
                    byte[] realData = new byte[dataLen];

                    //获取数据
                    buffer.get(realData);
                    //更新读指针
                    readPoint = buffer.position();

                    //提交到线程池解析数据
                    service.submit(() -> {
                        Datagram datagram = datagramUtil.decode(realData);
                        eventCenter.listen(SocketEvent.RECEIVE, datagram);
                    });
                } else {
                    logger.debug("当前数据不够长，等待下一次读取");
                    break;
                }
            } finally {
                //检查是否需要扩容
                checkGrow(false);
            }
        }
    }

    /**
     * 检查是否需要扩容
     *
     * @param grow 扩容参数，如果发现需要扩容，当该参数为true时会直接扩容，当该参数为false时会先整理内存
     */
    private void checkGrow(boolean grow) {
        //扩容阀值，1/8
        int threshold = bufferSize >> 3;
        int writeable = buffer.capacity() - writePoint;
        //判断可写空间是否到达扩容阀值
        if (writeable <= threshold) {
            if (grow) {
                //扩容四分之一后直接返回
                buffer = CollectionUtil.grow(buffer, readPoint, writePoint - readPoint, bufferSize >> 2);
                bufferSize = buffer.capacity();
                writeable = buffer.capacity() - writePoint;
                threshold = bufferSize >> 3;
                if (writeable <= threshold) {
                    throw new OutOfMemoryError("缓冲区已经达到最大，无法扩容且现有数据无法删除");
                }
            } else {
                //到达扩容阀值，首先整理空间
                buffer.position(readPoint);
                buffer.compact();
                readPoint = 0;
                writePoint = buffer.position();
                //整理完毕后再次检查
                checkGrow(true);
            }
        }
    }

    @Override
    public synchronized boolean shutdown() {
        if (super.shutdown()) {
            this.service.shutdown();
            return true;
        } else {
            return false;
        }
    }
}
