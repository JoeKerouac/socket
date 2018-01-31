package com.joe.easysocket.server.backserver.mvc.impl;

import com.joe.easysocket.server.backserver.Config;
import com.joe.easysocket.server.backserver.mvc.DataWorker;
import com.joe.easysocket.server.backserver.mvc.impl.coder.DataReaderContainer;
import com.joe.easysocket.server.backserver.mvc.impl.coder.DataWriterContainer;
import com.joe.easysocket.server.backserver.mvc.impl.coder.ReaderInterceptor;
import com.joe.easysocket.server.backserver.mvc.impl.coder.WriterInterceptor;
import com.joe.easysocket.server.backserver.mvc.impl.container.BeanContainerImpl;
import com.joe.easysocket.server.backserver.mvc.impl.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.ResponseContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.session.Session;
import com.joe.easysocket.server.backserver.mvc.impl.context.session.SessionManager;
import com.joe.easysocket.server.backserver.mvc.impl.context.session.SessionManagerImpl;
import com.joe.easysocket.server.backserver.mvc.impl.data.BaseDTO;
import com.joe.easysocket.server.backserver.mvc.impl.data.InterfaceData;
import com.joe.easysocket.server.backserver.mvc.impl.exception.FilterException;
import com.joe.easysocket.server.backserver.mvc.impl.exception.MediaTypeNoSupportException;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamParserException;
import com.joe.easysocket.server.backserver.mvc.impl.exception.ResourceNotFoundException;
import com.joe.easysocket.server.backserver.mvc.impl.exceptionmapper.ExceptionMapper;
import com.joe.easysocket.server.backserver.mvc.impl.exceptionmapper.ExceptionMapperContainer;
import com.joe.easysocket.server.backserver.mvc.impl.filter.FilterContainer;
import com.joe.easysocket.server.backserver.mvc.impl.param.ParamParserContainer;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Resource;
import com.joe.easysocket.server.backserver.mvc.impl.resource.ResourceContainer;
import com.joe.easysocket.server.common.config.ClusterConfig;
import com.joe.easysocket.server.common.data.Datagram;
import com.joe.easysocket.server.common.data.DatagramUtil;
import com.joe.easysocket.server.common.data.ProtocolData;
import com.joe.easysocket.server.common.exception.NoRequireParamException;
import com.joe.easysocket.server.common.msg.DataMsg;
import com.joe.easysocket.server.common.spi.PublishCenter;
import com.joe.utils.common.StringUtils;
import com.joe.utils.parse.json.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ValidationException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据处理器，处理协议栈传过来的数据，并且给出结果（目前异常处理的时候没有处理异常处理器异常的情况）
 * <p>
 * 该实现中register为系统调用，手动调用无效
 * <p>
 * 其中BeanContainer可以提供最基础的实现，但是建议自己实现并注册，默认容器仅用于测试
 * <p>
 * 注意：该类非线程安全
 *
 * @author joe
 */
public class MvcDataworker implements DataWorker {
    private static final Logger logger = LoggerFactory.getLogger(MvcDataworker.class);
    private static final JsonParser parser = JsonParser.getInstance();
    private ResourceContainer resourceContainer;
    private FilterContainer filterContainer;
    private DataWriterContainer dataWriterContainer;
    private DataReaderContainer dataReaderContainer;
    private ExceptionMapperContainer exceptionMapperContainer;
    private SessionManager sessionManager;
    private BeanContainer beanContainer;
    private ParamParserContainer paramParserContainer;
    //发布中心
    private PublishCenter publishCenter;
    //执行任务的线程池
    private ExecutorService service;
    //关闭标志，为true时表示关闭
    private volatile boolean shutdown = true;
    /**
     * 数据处理线程名的格式
     */
    private String threadName;
    /**
     * 最大线程数
     */
    private int maxThread;
    /**
     * 最小线程数
     */
    private int minThread;
    /**
     * 空闲线程存活时间，单位为秒
     */
    private long threadAliveTime;

    /**
     * 默认构造器
     *
     * @param config 配置
     * @param id     后端id
     */
    public MvcDataworker(Config config, String id) {
        ClusterConfig clusterConfig = config.getClusterConfig();
        this.sessionManager = new SessionManagerImpl(id, config);
        this.beanContainer = config.getBeanContainer() == null ? new BeanContainerImpl("com") : config
                .getBeanContainer();
        this.publishCenter = clusterConfig.getPublishCenter();
        this.maxThread = config.getMaxThreadCount();
        this.maxThread = this.maxThread <= 0 ? Runtime.getRuntime().availableProcessors() * 150 : this.maxThread;
        this.minThread = config.getMinThreadCount();
        this.minThread = this.minThread <= 0 ? Runtime.getRuntime().availableProcessors() * 50 : this.minThread;
        this.threadAliveTime = config.getThreadAliveTime();
        this.threadAliveTime = this.threadAliveTime <= 0 ? 30 : this.threadAliveTime;
        this.threadName = config.getThreadName();
        //校验格式是否正确
        String.format(threadName, Integer.valueOf(0));
    }

    @Override
    public synchronized void start() {
        if (!shutdown) {
            logger.debug("服务器已经启动，请勿重复启动");
            return;
        }
        init();
        logger.debug("服务器启动成功");
        shutdown = false;
        return;
    }

    @Override
    public synchronized void shutdown() {
        if (shutdown) {
            logger.debug("服务器已经关闭，请勿重复关闭");
            return;
        }

        logger.info("销毁MVC数据处理器");
        logger.debug("关闭正在运行的数据处理，等待当前数据处理的完成");
        service.shutdown();
        logger.debug("数据处理完成，关闭数据处理服务");

        beanContainer.shutdown();
        sessionManager.shutdown();

        resourceContainer.shutdown();
        filterContainer.shutdown();
        dataWriterContainer.shutdown();
        dataReaderContainer.shutdown();
        exceptionMapperContainer.shutdown();
        paramParserContainer.shutdown();
        logger.info("MVC数据处理器销毁成功");
        shutdown = true;
    }

    private void init() {
        logger.info("开始初始化MVC数据处理器");
        //检查是否有队列和发布中心
        if (this.publishCenter == null) {
            logger.error("协议栈缺少队列或者发布中心，请先注册队列或者发布中心");
            throw new NoRequireParamException(this.publishCenter == null ? "PublishCenter" : null);
        }

        //判断bean容器是否为null，不为null则使用默认
        this.beanContainer = beanContainer == null ? new BeanContainerImpl("com") : this.beanContainer;

        //构建数据处理线程池
        ThreadFactory factory = new ThreadFactory() {
            AtomicInteger counter = new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                return new Thread(r, String.format(threadName, this.counter.getAndAdd(1)));
            }
        };

        //构建线程池
        logger.debug("构建线程池，参数为{}:{}:{}:{}", minThread, maxThread, threadAliveTime, threadName);
        service = new ThreadPoolExecutor(minThread, maxThread, threadAliveTime, TimeUnit.SECONDS, new
                LinkedBlockingQueue(), factory);
        logger.debug("线程池构建完成");


        service.submit(() -> {
            //不加该行service中没有提交过任务时当程序运行完毕会自动关闭，只有这样该线程组才不会自动关闭，后端项目才不
            // 会启动后就立即自动关闭
            logger.debug("线程组启动成功，线程组配置为");
        });

        beanContainer.start();
        sessionManager.start();
        resourceContainer = new ResourceContainer(beanContainer);
        filterContainer = new FilterContainer(beanContainer);
        dataWriterContainer = new DataWriterContainer(beanContainer);
        paramParserContainer = new ParamParserContainer(beanContainer);
        dataReaderContainer = new DataReaderContainer(beanContainer, paramParserContainer);
        exceptionMapperContainer = new ExceptionMapperContainer(beanContainer);

        resourceContainer.start();
        filterContainer.start();
        dataWriterContainer.start();
        dataReaderContainer.start();
        exceptionMapperContainer.start();
        paramParserContainer.start();
        logger.info("MVC数据处理器初始化完毕");
    }

    @Override
    public void read(DataMsg msg) {
        try {
            logger.debug("读取到数据为：{}", msg);
            if (msg == null) {
                logger.warn("接收到的数据为空，不处理");
                return;
            }
            ProtocolData protocolData = msg.getData();
            byte[] data = protocolData.getData();

            Datagram datagram = DatagramUtil.decode(data);
            if (worker(datagram.getType())) {
                logger.debug("该数据可以处理，提交到线程池开始处理");
                service.submit(() -> {
                    ProtocolData result = accept(datagram, msg.getSrc(), protocolData.getChannel(), msg.getTopic(),
                            protocolData
                                    .getHost(), protocolData.getPort(), protocolData.getReqTime());
                    logger.debug("MVC数据处理器处理{}的结果为：{}；将该结果发送至底层，对应的通道信息为：{}", datagram, result, protocolData
                            .getChannel());
                    msg.setData(result);
                    publishCenter.pub(msg.getRespTopic(), msg);
                });
            }
        } catch (Throwable e) {
            logger.error("数据处理中发生异常，数据为：{}", msg, e);
        }
    }

    private ProtocolData accept(Datagram datagram, String balanceId, String channel, String topic, String host, int
            port, long reqTime) {
        logger.debug("接收到数据，开始处理。{}，对应的channelinfo为：{}:{}:{}:{}", datagram, topic, channel, host, port);
        RequestContext requestContext = null;
        ResponseContext responseContext = null;
        InterfaceData resultData;
        InterfaceData message = null;
        try {
            Datagram requestDatagram = datagram;
            byte[] body = requestDatagram.getBody();

            if (body == null || body.length == 0) {
                //请求必须有请求体，否则最基本的invoke信息都没有
                logger.warn("该请求没有请求体，请求内容：{}", datagram);
                return null;
            }

            //必须在此处初始化，否则当发生异常的时候异常中获取到的requestContext是空，无法获取到信息
            requestContext = new RequestContext(channel, topic, requestDatagram, requestDatagram.getCharset());
            // 构建响应上下文，必须在此处初始化，否则当发生异常的时候异常中获取到的responseContext是空，无法获取到信息
            responseContext = new ResponseContext();
            // MVC数据处理器只有这一种请求data，直接读取
            logger.debug("开始解析请求数据");
            message = parser.readAsObject(body, InterfaceData.class);
            logger.debug("请求数据解析完毕，请求数据为：{}", message);

            logger.debug("开始构建请求上下文");
            // 构建请求上下文
            Session session = sessionManager.get(channel, balanceId, port, host);
            requestContext.setSession(session);
            logger.debug("请求上下文构建成功，开始查找请求的资源");


            // 搜索指定的resource
            Resource resource = findResource(message.getInvoke());
            // 放进请求上下文
            requestContext.setResource(resource);
            logger.debug("请求的资源查找完成，请求资源为：{}", resource);

            // 开始查找数据编码器
            logger.debug("开始查找数据编码器");
            // 找到请求数据编码处理器
            ReaderInterceptor readerInterceptor = findReaderInterceptor(resource.getConsume());
            requestContext.setReader(readerInterceptor);
            logger.debug("数据编码器为：{}", readerInterceptor);

            logger.debug("开始解码参数");
            // 开始解码数据
            Object[] param = readerInterceptor.read(resource.getParams(), requestContext, message.getData());
            requestContext.setParams(param);
            logger.debug("参数解码完毕，参数为：{}", param);

            logger.debug("开始验证参数");
            resource.check(param);
            logger.debug("参数验证完毕");

            logger.debug("开始请求filter");
            // 请求filter
            filterContainer.requestFilter(requestContext.getRequest());
            logger.debug("filter完毕，开始调用资源");

            // 调用资源
            Object result = resource.invoke(requestContext);
            responseContext.getResponse().setResult(result);
            logger.debug("资源调用完毕，请求结果为：{}", result);

            // 响应
            logger.debug("开始处理响应");
            resultData = response(requestContext, responseContext, message);
            logger.debug("响应处理完毕");
        } catch (ResourceNotFoundException e) {
            logger.error("用户请求的资源不存在", e);
            resultData = buildResult(requestContext.getSource(), new BaseDTO<>("404"), message.getId(), message
                    .getInvoke(), findWriterInterceptor(null));
        } catch (MediaTypeNoSupportException e) {
            logger.error("找不到对应的参数解析器", e);
            resultData = buildResult(requestContext.getSource(), new BaseDTO<>("505"), message.getId(), message
                    .getInvoke(), resolveDataInterceptor(requestContext, responseContext));
        } catch (ParamParserException e) {
            logger.error("参数解析失败", e);
            resultData = buildResult(requestContext.getSource(), BaseDTO.buildError("400"), message.getId(), message
                    .getInvoke(), resolveDataInterceptor(requestContext, responseContext));
        } catch (ValidationException e) {
            logger.error("参数验证失败", e);
            resultData = buildResult(requestContext.getSource(), BaseDTO.buildError("505"), message.getId(), message
                    .getInvoke(), resolveDataInterceptor(requestContext, responseContext));
        } catch (Throwable e) {
            // 请求过程中发生了异常
            logger.error("请求过程中发生了异常，开始查找相应的异常处理器处理异常", e);

            // 查找异常处理器
            List<ExceptionMapper> exceptionMappers = exceptionMapperContainer.select(mapper -> {
                return mapper.mapper(e);
            });

            logger.info("异常处理器查找完毕");
            if (exceptionMappers.isEmpty()) {
                logger.error("异常没有找到相应的处理器", e);
                throw e;
            } else {
                logger.info("找到异常处理器，由相应的异常处理器处理");
                ResponseContext.Response response = exceptionMappers.get(0).toResponse(e);
                resultData = new InterfaceData(message.getId(), message.getInvoke(), resolveDataInterceptor
                        (requestContext, responseContext).write(response.getResult()));
            }
        }

        return new ProtocolData(DatagramUtil.build(parser.toJson(resultData).getBytes(), (byte) 3, (byte) 1).getData
                (), port, host, channel, reqTime, System.currentTimeMillis());
    }

    /**
     * 响应处理
     *
     * @param requestContext  请求上下文
     * @param responseContext 响应上下文
     * @param userData        用户发来的请求数据
     * @return 响应数据
     * @throws MediaTypeNoSupportException 找不到相应数据的编码器
     * @throws FilterException             filter异常
     */
    private InterfaceData response(RequestContext requestContext, ResponseContext responseContext, InterfaceData
            userData) throws MediaTypeNoSupportException, FilterException {
        logger.debug("开始构建响应");
        RequestContext.RequestWrapper request = requestContext.getRequest();
        Resource resource = requestContext.getResource();
        ResponseContext.Response response = responseContext.getResponse();
        //该消息的来源
        String src = requestContext.getSource();

        // 请求过程中没有发生异常
        // 响应filter
        filterContainer.responseFilter(request, response);

        // 找到处理响应的编码器
        WriterInterceptor writerInterceptor = findWriterInterceptor(resource.getProduce());
        responseContext.setWriter(writerInterceptor);

        Object result = response.getResult();

        logger.debug("请求结果为：{}", result);

        // 根据不同的结果分别处理，资源有可能没有返回，有可能返回聊天消息，也有可能返回正常的数据
        if (result == null) {
            logger.debug("请求的接口{}没有响应消息，返回一个BaseDTO", userData.getInvoke());
            // 如果该接口没有响应消息，那么返回一个基本的请求成功
            InterfaceData data = buildResult(src, BaseDTO.buildSuccess(), userData.getId(), userData.getInvoke(),
                    writerInterceptor);
            return data;
        } else if (result instanceof InterfaceData) {
            logger.debug("用户响应的信息是InterfaceData对象");
            InterfaceData data = (InterfaceData) result;
            return data;
        } else {
            logger.debug("请求接口{}的响应信息为：{}", userData.getInvoke(), result);
            // 该接口有响应消息并且不是聊天类型消息，那么直接将该消息返回
            InterfaceData data = buildResult(src, result, userData.getId(), userData.getInvoke(), writerInterceptor);
            return data;
        }
    }

    /**
     * 构建响应结果
     *
     * @param srcOrDest         消息来源或者目的地
     * @param result            响应的结果
     * @param id                对应的请求消息的ID
     * @param invoke            对应的请求消息请求的接口名
     * @param writerInterceptor 对应的数据处理器
     * @return 响应结果
     */
    private InterfaceData buildResult(String srcOrDest, Object result, String id, String invoke, WriterInterceptor
            writerInterceptor) {
        invoke = invoke.startsWith("/") ? "/back" + invoke : "/back/" + invoke;
        if (writerInterceptor == null) {
            return null;
        }
        return new InterfaceData(id, invoke, writerInterceptor.write(result));
    }

    /**
     * 查找数据解码器
     *
     * @param consume 待读取内容的类型
     * @return 指定类型对应的数据解码器
     * @throws MediaTypeNoSupportException 找不到指定类型数据解码器时抛出该异常
     */
    private ReaderInterceptor findReaderInterceptor(String consume) throws MediaTypeNoSupportException {
        logger.debug("要查找的数据解码器类型为：{}", consume);
        final String realConsume = StringUtils.isEmpty(consume) ? "json" : consume;
        List<ReaderInterceptor> readerInterceptors = dataReaderContainer.select(reader -> {
            return reader.isReadable(realConsume);
        });

        logger.debug("数据编码器为：{}", readerInterceptors);
        if (readerInterceptors.isEmpty()) {
            // 找不到支持
            throw new MediaTypeNoSupportException(consume);
        }

        return readerInterceptors.get(0);
    }

    /**
     * 查找resource
     *
     * @param path resource的名字
     * @return 要查找的resource
     * @throws ResourceNotFoundException 找不到指定resource时抛出该异常
     */
    private Resource findResource(String path) throws ResourceNotFoundException {
        if (StringUtils.isEmpty(path)) {
            logger.error("请求信息中没有请求资源的名字");
            throw new ResourceNotFoundException(String.valueOf(path));
        }
        Resource resource = resourceContainer.findResource(path);
        logger.debug("请求的资源为：{}", resource);

        if (resource == null) {
            // 找不到要请求的resource
            logger.error("没有找到{}对应的资源，请检查请求地址是否有误", path);
            throw new ResourceNotFoundException(path);
        }
        return resource;
    }

    /**
     * 查找数据编码器
     *
     * @param produce 要处理的数据的类型
     * @return 指定类型对应的数据编码器
     * @throws MediaTypeNoSupportException 找不到指定编码器时抛出该异常
     */
    private WriterInterceptor findWriterInterceptor(String produce) throws MediaTypeNoSupportException {
        logger.debug("查找{}格式的数据编码器", produce);
        final String dataProduce = StringUtils.isEmpty(produce) ? "json" : produce;

        List<WriterInterceptor> writerInterceptors = dataWriterContainer.select(dataInterceptor -> {
            return dataInterceptor.isWriteable(dataProduce);
        });

        if (writerInterceptors.isEmpty()) {
            // 找不到支持
            throw new MediaTypeNoSupportException(String.valueOf(dataProduce));
        }
        return writerInterceptors.get(0);
    }

    /**
     * 确定一个响应数据处理器
     *
     * @param requestContext  请求上下文
     * @param responseContext 响应上下文
     * @return 响应数据处理器，该方法肯定会返回一个响应数据处理器
     */
    private WriterInterceptor resolveDataInterceptor(RequestContext requestContext, ResponseContext responseContext) {
        logger.debug("根据上下文确定一个响应数据处理器");
        WriterInterceptor writer = responseContext.getWriter();
        if (writer != null) {
            logger.debug("响应过程中已经确定了响应数据处理器，直接返回");
            return writer;
        } else {
            Resource resource = requestContext.getResource();
            try {
                return findWriterInterceptor(resource.getProduce());
            } catch (Exception e) {
                return findWriterInterceptor(null);
            }
        }
    }

    private boolean worker(int type) {
        return type == 1;
    }
}
