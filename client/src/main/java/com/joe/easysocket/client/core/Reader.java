package com.joe.easysocket.client.core;


import com.joe.easysocket.client.common.DatagramUtil;
import com.joe.easysocket.client.data.Datagram;
import com.joe.easysocket.client.data.InterfaceData;
import com.joe.easysocket.client.ext.InternalLogger;
import com.joe.easysocket.client.ext.Logger;
import com.joe.easysocket.client.ext.Serializer;

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
    //事件监听器
    private EventListener listener;
    //socket输入流
    private SocketChannel channel;
    //缓冲区大小
    private int bufferSize;
    //线程池
    private ExecutorService service;


    /**
     * 读取器构造器
     *
     * @param channel    socket的输入流
     * @param logger     日志对象
     * @param listener   事件监听器
     * @param callback   关闭回调
     * @param serializer 序列化器
     */
    public Reader(SocketChannel channel, Logger logger, EventListener listener, Callback
            callback, Serializer serializer) {
        super(logger instanceof InternalLogger ? logger : InternalLogger.getLogger(logger, Reader.class), callback,
                serializer);
        this.channel = channel;
        this.listener = listener;
        this.bufferSize = 1024;
        this.service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
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
        //缓冲区，暂时用1M的缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        //标记当前读取位置
        buffer.mark();

        while (!isShutdown()) {
            int readLen = channel.read(buffer);
            if (readLen == -1) {
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
        while (true) {
            //当前剩余可写位置
            final int writeable = buffer.capacity() - buffer.position();

            if (writeable <= 100) {
                System.out.println("不够用了");
            }


            //当前写指针位置，用于重置
            int writePoint = buffer.position();
            //回到上次读取的位置
            buffer.reset();
            //当前的读指针
            final int readPoint = buffer.position();
            //获取当前可读取的内容
            final int readable = writePoint - readPoint;

            try {
                //数据报长度，包含请求头
                int dataLen;

                if (readable >= headLength) {
                    byte[] header = new byte[headLength];
                    buffer.get(header);
                    //重置指针位置
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
                    //标记本次读取的位置
                    buffer.mark();

                    service.submit(() -> {
                        Datagram datagram = datagramUtil.decode(realData);
                        logger.debug("解析出来的数据报为[" + datagram + "]");
                        if (datagram.getType() == 0) {
                            logger.debug("收到的数据报为心跳包，忽略处理");
                            return;
                        }

                        logger.debug("开始解析数据报 [" + datagram + "] 的body");
                        InterfaceData data = serializer.read(datagram.getBody(), InterfaceData.class);
                        logger.debug("解析出来的数据报body为：" + data);
                        listener.listen(SocketEvent.RECEIVE, data);
                    });
                } else {
                    logger.debug("当前数据不够长，等待下一次读取");
                    break;
                }
            } finally {
                //暂时不考虑重置缓冲区后仍然不够用的问题，也就是当前可接受最大的数据是缓冲区的大小，超过就是出问题
                int len = 100;
                if (writeable <= len) {
                    logger.debug("当前缓冲区还有不到 [" + len + "] byte就要用完了，重置缓冲区");
                    writePoint = writePoint - buffer.position();
                    buffer.compact();
                    //需要重置指针
                    buffer.position(0);
                    buffer.mark();
                }
                //切换到写指针
                buffer.position(writePoint);
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
