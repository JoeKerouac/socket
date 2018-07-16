package com.joe.easysocket.server.backserver.mvc.impl.context;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.joe.easysocket.server.backserver.mvc.coder.DataReader;
import com.joe.easysocket.server.backserver.mvc.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.context.Session;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Resource;
import com.joe.utils.protocol.Datagram;

import lombok.Data;

/**
 * 请求上下文
 *
 * @author joe
 */
@Data
public class HttpRequestContext implements RequestContext {
    // 请求来源
    private final String         source;
    // 请求body
    private final byte[]         data;
    //请求数据包
    private final Datagram       datagram;
    // 请求开始时间
    private final long           beginTime;
    // 请求
    private final RequestWrapper request;
    // 解析后的参数
    private Object[]             params;
    // 请求的资源
    private Resource             resource;
    // 请求结束时间
    private long                 endTime;
    //本次请求对应的session
    private Session              session;
    //本次请求编码
    private final String         charset;
    // 数据编码器
    private DataReader           reader;
    //前端接收数据的topic
    private String               topic;

    /**
     * @param source   对应的通道ID
     * @param topic    对应的前端接收数据的topic
     * @param datagram 本次请求对应的数据报
     * @param charset  本次请求的编码
     */
    public HttpRequestContext(String source, String topic, Datagram datagram, String charset) {
        this.beginTime = System.currentTimeMillis();
        this.source = source;
        this.data = datagram.getBody();
        this.datagram = datagram;
        this.charset = charset;
        this.request = new RequestWrapper(this);
        this.topic = topic;
    }

    /**
     * 请求信息简单封装
     *
     * @author joe
     */
    public static class RequestWrapper {
        private final HttpRequestContext requestContext;
        private InputStream              inputStream;
        private Object[]                 entity;

        private RequestWrapper(HttpRequestContext requestContext) {
            this.requestContext = requestContext;
        }

        /**
         * 获取请求对应的session
         *
         * @return 请求对应的session
         */
        public Session getSession() {
            return requestContext.getSession();
        }

        /**
         * 获取请求数据的编码信息
         *
         * @return 请求数据的编码信息，可以用来编码从输入流中读取的数据
         */
        public String getCharset() {
            return requestContext.getCharset();
        }

        /**
         * 获取解析后的参数，参数为一个Object数组
         *
         * @return 请求数据解析后的参数，类型为Object数组，数组顺序与接口声明的顺序一致
         */
        public synchronized Object[] getEntity() {
            if (entity == null) {
                entity = Arrays.stream(requestContext.getParams())
                    .filter(
                        param -> !(param instanceof HttpRequestContext || param instanceof Session
                                   || param instanceof HttpResponseContext))
                    .collect(Collectors.toList()).toArray();
            }
            return entity;
        }

        /**
         * 获取请求数据的输入流
         *
         * @return 请求数据对应的输入流
         */
        public synchronized InputStream getInputStream() {
            if (inputStream == null) {
                inputStream = new ByteArrayInputStream(requestContext.data);
            }
            return inputStream;
        }
    }
}
