package com.joe.easysocket.server.backserver.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joe.easysocket.server.backserver.Config;
import com.joe.easysocket.server.backserver.mvc.MvcController;
import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.context.Session;
import com.joe.easysocket.server.backserver.mvc.context.SessionManager;
import com.joe.easysocket.server.backserver.mvc.impl.MvcControllerImpl;
import com.joe.easysocket.server.backserver.mvc.impl.container.BaseBeanContainer;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.session.SessionManagerImpl;
import com.joe.easysocket.server.backserver.spi.DataWorker;
import com.joe.easysocket.server.common.config.Const;
import com.joe.easysocket.server.common.config.Environment;
import com.joe.easysocket.server.common.data.ProtocolData;
import com.joe.easysocket.server.common.exception.NoRequireParamException;
import com.joe.easysocket.server.common.msg.DataMsg;
import com.joe.easysocket.server.common.spi.MessageCenter;
import com.joe.utils.protocol.Datagram;
import com.joe.utils.protocol.DatagramUtil;
import com.joe.utils.reflect.clazz.ClassUtils;
import com.joe.utils.serialize.json.JsonParser;

/**
 * MVC数据处理器
 *
 * @author joe
 * @version 2018.03.06 11:44
 */
public class MvcDataworker implements DataWorker {
    private static final Logger     logger   = LoggerFactory.getLogger(MvcDataworker.class);
    private static final JsonParser parser   = JsonParser.getInstance();
    /**
     * 发布中心
     */
    private MessageCenter           messageCenter;
    /**
     * 服务器是否关闭标志，为true时表示关闭
     */
    private volatile boolean        shutdown = true;
    /**
     * MVC数据处理器内核
     */
    private MvcController           mvcController;
    /**
     * session管理器
     */
    private SessionManager          sessionManager;

    /**
     * 默认构造器
     *
     * @param environment 系统环境
     * @param id          后端id
     */
    public MvcDataworker(Environment environment, String id) {
        Config config = environment.get(Const.CONFIG);
        this.sessionManager = new SessionManagerImpl(id, environment);
        this.messageCenter = environment.get(Const.MSG_CENTER);
        BeanContainer beanContainer = config.getBeanContainer();
        if (beanContainer == null) {
            beanContainer = new BaseBeanContainer(ClassUtils.getDefaultClassLoader(), "com");
        }
        int maxThread = config.getMaxThreadCount();
        int minThread = config.getMinThreadCount();
        long threadAliveTime = config.getThreadAliveTime();
        this.mvcController = new MvcControllerImpl(sessionManager, beanContainer, maxThread,
            minThread, threadAliveTime);
    }

    @Override
    public synchronized void start() {
        if (!shutdown) {
            logger.debug("服务器已经启动，请勿重复启动");
            return;
        }
        //检查是否有队列和发布中心
        if (this.messageCenter == null) {
            logger.error("协议栈缺少队列或者发布中心，请先注册队列或者发布中心");
            throw new NoRequireParamException(this.messageCenter == null ? "MessageCenter" : null);
        }

        sessionManager.start();
        mvcController.start();
        logger.debug("服务器启动成功");
        shutdown = false;
    }

    @Override
    public synchronized void shutdown() {
        if (shutdown) {
            logger.debug("服务器已经关闭，请勿重复关闭");
            return;
        }

        sessionManager.shutdown();
        mvcController.shutdown();
        shutdown = true;
    }

    @Override
    public void read(DataMsg msg) {
        try {
            logger.debug("读取到数据为：{}", msg);
            if (msg == null) {
                logger.warn("接收到的数据为空，不处理");
                return;
            }

            //获取信息
            ProtocolData protocolData = msg.getData();
            byte[] data = protocolData.getData();
            String host = protocolData.getHost();
            int port = protocolData.getPort();
            String channel = protocolData.getChannel();
            String balanceId = msg.getSrc();
            Datagram datagram = DatagramUtil.decode(data);

            logger.debug("构建session和requestContext");
            //构建session和HttpRequestContext
            Session session = sessionManager.get(channel, balanceId, port, host);
            HttpRequestContext requestContext = new HttpRequestContext(channel, msg.getTopic(),
                datagram, datagram.getCharset());
            requestContext.setSession(session);
            logger.debug("session和requestContext构建完成");

            logger.debug("将数据放入MVC处理器内核中处理");
            mvcController.deal(datagram, requestContext, result -> {
                logger.debug("MVC处理器内核处理完毕，结果为：{}", result);
                ProtocolData resultData = new ProtocolData(
                    DatagramUtil.build(parser.toJson(result).getBytes(), (byte) 3, (byte) 1)
                        .getData(),
                    port, host, protocolData.getChannel(), protocolData.getReqTime(),
                    System.currentTimeMillis());

                logger.debug("MVC数据处理器处理{}的结果为：{}；将该结果发送至底层，对应的通道信息为：{}", datagram, result,
                    protocolData.getChannel());
                msg.setData(resultData);
                //响应数据
                messageCenter.pub(msg.getRespTopic(), msg);
            });
        } catch (Throwable e) {
            logger.error("数据处理中发生异常，数据为：{}", msg, e);
        }
    }
}
