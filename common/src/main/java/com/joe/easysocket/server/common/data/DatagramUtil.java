package com.joe.easysocket.server.common.data;

import com.joe.easysocket.server.common.exception.DataOutOfMemory;
import com.joe.easysocket.server.common.exception.IllegalDataException;
import com.joe.easysocket.server.common.exception.IllegalRequestException;
import com.joe.utils.collection.ByteArray;
import com.joe.utils.common.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * 数据报工具
 *
 * @author joe
 */
public class DatagramUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatagramUtil.class);
    // 数据报数据除去请求头的最大长度
    private static final int MAX_LENGTH = Datagram.MAX_LENGTH - Datagram.HEADER;

    /**
     * 根据要发送的数据构建数据报（编码采用当前系统默认编码）
     *
     * @param body    要发送的数据
     * @param type    数据报类型（0：心跳包；1：内置MVC数据处理器数据类型；2：文件传输；3：ACK；4：后端主动发往前端的数据）
     * @param version 数据报版本
     * @return 构建好的数据报对象
     * @throws DataOutOfMemory 当数据长度过长时会抛出该异常
     */
    public static Datagram build(byte[] body, byte type, byte version) throws DataOutOfMemory {
        int dataLen = 0;
        if (body != null && body.length != 0) {
            // 获取要发送的数据的长度
            dataLen = body.length;
            logger.debug("要构建的数据报的body长度为：{}", dataLen);
        } else {
            logger.debug("要构建一个空的数据报");
        }

        logger.debug("要发送的数据为：{}", body);
        if (dataLen > MAX_LENGTH) {
            // 数据报超出最大值
            logger.error("数据报数据长度超过最大值：{}", MAX_LENGTH);
            throw new DataOutOfMemory(String.format("数据长度超过最大值%d", MAX_LENGTH));
        }
        // 缓存
        ByteArray buffer = new ByteArray(Datagram.HEADER + dataLen);
        // 一个字节的版本号
        buffer.append(version);
        // 四个字节的body长度
        buffer.append(convert(dataLen));
        // 一个字节的数据类型
        buffer.append(type);
        // 十个字节的字符集，字符集为当前系统默认字符集不可更改，不足十个字节的用0填充
        String charset = Charset.defaultCharset().name();
        ByteArray charsetBuffer = new ByteArray(10);
        charsetBuffer.append(charset.getBytes());
        if (charsetBuffer.size() > 10) {
            throw new DataOutOfMemory("数据报字符集长度最大为10byte，当前系统默认字符集超过该长度");
        }

        // 字符集不足10位的补0
        while (charsetBuffer.size() < 10) {
            charsetBuffer.append((byte) 0);
        }
        buffer.append(charsetBuffer.getData());


        //添加ID字段
        String id = Tools.createUUID();
        ByteArray idByte = new ByteArray(40);
        idByte.append(id.getBytes());
        for (int i = 0; i < 40 - id.getBytes().length; i++) {
            idByte.append((byte) 0);
        }
        byte[] ids = idByte.getData();
        buffer.append(ids);

        if (dataLen != 0) {
            // 填充数据
            buffer.append(body);
        }

        Datagram datagram = new Datagram(buffer.getData(), dataLen, body, version, charset, type, ids);
        logger.debug("转换后的数据报是：{}", datagram);
        return datagram;
    }

    /**
     * 数据报解析，将byte数组解析为数据报
     *
     * @param data 数据报的byte数组
     * @return 从data中解析的数据报对象
     * @throws IllegalRequestException 正常情况不会抛该异常，当请求非法时可能抛出该异常
     */
    public static Datagram decode(final byte[] data) throws IllegalRequestException {
        try {
            logger.debug("要解析的数据为：{}", data);
            // 字符集数据
            ByteArray charsetData = new ByteArray(10);
            for (int i = 6; i < 16; i++) {
                if (data[i] != 0) {
                    charsetData.append(data[i]);
                } else {
                    break;
                }
            }
            // 字符集
            final String charset = new String(charsetData.getData());
            // 版本号
            final byte version = data[0];
            // 数据报数据类型
            final byte type = data[5];
            // 长度
            final int len = convert(data, 1);
            logger.debug("要解析的数据报的字符集为：{}，版本号为：{}，数据报类型为：{}", charset, version, type);

            byte[] buffer;
            if ((data.length - Datagram.HEADER) != len) {
                logger.warn("数据报head中的长度字段为：{}，数据报body的实际长度为：{}", len, data.length - Datagram.HEADER);
                throw new IllegalDataException("数据报head中的长度字段为：" + len + "，数据报body的实际长度为：" + (data.length - Datagram
                        .HEADER));
            } else {
                buffer = data;
            }

            logger.debug("获取id字段");
            byte[] idByte = new byte[40];
            System.arraycopy(data, 16, idByte, 0, idByte.length);
            String id = new String(idByte);
            logger.debug("数据报ID为：{}", id);

            // 有可能是空报文的数据报
            if (len == 0) {
                logger.debug("要解析的数据中head标志body长度为0，直接返回一个空body的datagram对象");
                Datagram datagram = new Datagram(buffer, len, null, version, charset, type, idByte);
                logger.debug("封装好的数据报body为：{}", datagram);
                return datagram;
            } else {
                // 真实的业务数据
                byte[] body = new byte[len];
                System.arraycopy(buffer, Datagram.HEADER, body, 0, body.length);
                Datagram datagram = new Datagram(buffer, len, body, version, charset, type, idByte);
                logger.debug("封装好的数据报body为：{}", datagram);
                return datagram;
            }
        } catch (Exception e) {
            logger.error("数据报解析错误，错误原因：{}", e);
            throw new IllegalRequestException(e);
        }
    }

    /**
     * 将一个int类型转换为四个字节的byte数组
     *
     * @param data int类型的参数
     * @return byte类型的数组
     */
    public static byte[] convert(int data) {
        long len = Integer.toUnsignedLong(data);
        byte[] b = new byte[4];
        b[0] = (byte) (len >> 24);
        b[1] = (byte) (len >> 16);
        b[2] = (byte) (len >> 8);
        b[3] = (byte) len;
        return b;
    }

    /**
     * 将四个字节转换为一个int类型的数字，从data中下标为0的数据开始
     *
     * @param data 四个字节的byte数组
     * @return 四个byte数组转换为的一个int
     */
    public static int convert(byte[] data) {
        return convert(data, 0);
    }

    /**
     * 将四个字节转换为一个int类型的数字
     *
     * @param data  data数据
     * @param start 四个字节长度开始的位置
     * @return 四个byte转换为的一个int
     */
    public static int convert(byte[] data, int start) {
        return (Byte.toUnsignedInt(data[start]) << 24) | (Byte.toUnsignedInt(data[start + 1]) << 16)
                | (Byte.toUnsignedInt(data[start + 2]) << 8) | Byte.toUnsignedInt(data[start + 3]);
    }
}
