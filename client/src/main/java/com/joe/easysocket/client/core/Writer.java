package com.joe.easysocket.client.core;

import com.joe.easysocket.client.data.Datagram;
import com.joe.easysocket.client.data.InterfaceData;
import com.joe.easysocket.client.ext.InternalLogger;
import com.joe.easysocket.client.ext.Logger;
import com.joe.easysocket.client.ext.Serializer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 数据发送器
 *
 * @author joe
 */
public class Writer extends Worker {
    //队列
    private BlockingDeque<Msg> queue;
    private OutputStream out;

    public Writer(OutputStream out, Logger logger, Serializer serializer, Callback
            callback) {
        super(logger instanceof InternalLogger ? logger : InternalLogger.getLogger(logger, Writer.class), callback,
                serializer);
        this.out = out;
        this.queue = new LinkedBlockingDeque<>();
    }

    /**
     * 启动数据发送器
     */
    public synchronized void start() {
        if (!isShutdown()) {
            throw new IllegalThreadStateException("请勿重复启动读取器");
        }
        shutdown = false;

        this.worker = new Thread(() -> {
            logger.info("数据发送器启动");
            try {
                write();
            } catch (IOException e) {
                logger.debug("发送器输出流发生异常，中断工作" + e);
            } catch (InterruptedException e) {
                logger.debug("发送器被外部中断，停止工作");
            } finally {
                if (!isShutdown()) {
                    shutdown();
                }
            }
        }, "数据发送器");
        this.worker.start();
    }


    /**
     * 往服务器发送数据
     *
     * @param invoke 要调用的接口名
     * @param data   要发送的接口数据的序列化
     * @return 返回true表示发送成功（并没有真正发送成功，只是加入了发送队列）
     */
    public boolean write(String invoke, String data) {
        try {
            queue.put(new Msg(invoke, data, false, null));
            return true;
        } catch (InterruptedException e) {
            logger.error("写入失败");
            return false;
        }
    }

    /**
     * 对消息ack
     *
     * @param id 消息ID
     * @return 返回true表示成功（成功加入队列）
     */
    public boolean ack(byte[] id) {
        try {
            queue.put(new Msg(null, null, true, id));
            return true;
        } catch (InterruptedException e) {
            logger.error("写入失败");
            return false;
        }
    }

    /**
     * 写数据（从队列读取，只要连接没有关闭就一直写）
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private void write() throws InterruptedException, IOException {
        while (!isShutdown()) {
            Msg msg = null;
            try {
                msg = queue.take();
                logger.debug("收到消息：" + msg + "，准备发往服务器");

                Datagram datagram;
                if (msg.getInvoke() == null) {
                    if (msg.isAck()) {
                        //ack包
                        datagram = datagramUtil.build(null, (byte) 0, (byte) 3, msg.getId());
                    } else {
                        //心跳包
                        datagram = datagramUtil.build(null, (byte) 0, (byte) 1);
                    }
                } else {
                    //普通接口请求包
                    InterfaceData interfaceData = new InterfaceData(String.valueOf(System.currentTimeMillis()), msg
                            .getInvoke(), msg.getData() != null ? new String(serializer.write(msg.getData())) : null);
                    datagram = datagramUtil.build(serializer.write(interfaceData), (byte) 1, (byte) 1);
                }
                logger.debug("消息封装为数据报后是：" + datagram);
                out.write(datagram.getData());
                out.flush();
            } catch (IOException | InterruptedException e) {
                //防止发送失败丢失数据
                if (msg != null) {
                    logger.warn("发送数据[" + msg + "]时发生异常，重新加入队列");
                    queue.put(msg);
                }
                throw e;
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class Msg {
        private String invoke;
        private Object data;
        /**
         * 是否是ACK，如果为true表示该消息是ack消息
         */
        private boolean ack = false;
        /**
         * ack消息ID
         */
        private byte[] id;
    }
}
