package com.joe.easysocket.server.backserver.mvc.impl;

import com.joe.easysocket.server.backserver.impl.MvcDataworker;
import com.joe.easysocket.server.backserver.mvc.MvcController;
import com.joe.easysocket.server.backserver.mvc.coder.DataReader;
import com.joe.easysocket.server.backserver.mvc.coder.DataReaderContainer;
import com.joe.easysocket.server.backserver.mvc.coder.DataWriter;
import com.joe.easysocket.server.backserver.mvc.coder.DataWriterContainer;
import com.joe.easysocket.server.backserver.mvc.container.BeanContainer;
import com.joe.easysocket.server.backserver.mvc.context.RequestContext;
import com.joe.easysocket.server.backserver.mvc.context.SessionManager;
import com.joe.easysocket.server.backserver.mvc.data.InterfaceData;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpRequestContext;
import com.joe.easysocket.server.backserver.mvc.impl.context.HttpResponseContext;
import com.joe.easysocket.server.backserver.mvc.impl.exception.*;
import com.joe.easysocket.server.backserver.mvc.impl.exceptionmapper.ExceptionMapper;
import com.joe.easysocket.server.backserver.mvc.impl.exceptionmapper.ExceptionMapperContainer;
import com.joe.easysocket.server.backserver.mvc.impl.filter.FilterContainer;
import com.joe.easysocket.server.backserver.mvc.impl.param.ParamParserContainer;
import com.joe.easysocket.server.backserver.mvc.impl.resource.Resource;
import com.joe.easysocket.server.backserver.mvc.impl.resource.ResourceContainer;
import com.joe.easysocket.server.common.data.Datagram;
import com.joe.utils.common.StringUtils;
import com.joe.utils.data.BaseDTO;
import com.joe.utils.parse.json.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * MVC控制器
 *
 * @author joe
 * @version 2018.03.06 10:34
 */
public class MvcControllerImpl implements MvcController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MvcDataworker.class);
    private static final JsonParser JSON_PARSER = JsonParser.getInstance();
    private ResourceContainer resourceContainer;
    private FilterContainer filterContainer;
    private DataWriterContainer dataWriterContainer;
    private DataReaderContainer dataReaderContainer;
    private ExceptionMapperContainer exceptionMapperContainer;
    private SessionManager sessionManager;
    private BeanContainer beanContainer;
    private ParamParserContainer paramParserContainer;
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
     * MVC控制器
     *
     * @param sessionManager  session管理器
     * @param beanContainer   bean容器，不能为null
     * @param maxThread       MVC处理器线程池的最大线程数
     * @param minThread       MVC处理器线程池的最小线程数
     * @param threadAliveTime MVC处理器线程池空闲线程存活时间，单位为秒
     */
    public MvcControllerImpl(SessionManager sessionManager, BeanContainer beanContainer, int maxThread, int minThread,
                             long threadAliveTime) {
        if (sessionManager == null || beanContainer == null) {
            throw new NullPointerException("sessionManager or beanContainer must not be null");
        }
        this.sessionManager = sessionManager;
        this.beanContainer = beanContainer;
        this.maxThread = maxThread <= 0 ? Runtime.getRuntime().availableProcessors() * 150 : maxThread;
        this.minThread = minThread <= 0 ? Runtime.getRuntime().availableProcessors() * 50 : minThread;
        this.threadAliveTime = threadAliveTime <= 0 ? 30 : threadAliveTime;
        this.threadName = "mvc处理线程%d";
    }

    @Override
    public synchronized void start() {
        if (!shutdown) {
            LOGGER.debug("服务器已经启动，请勿重复启动");
            return;
        }
        init();
        LOGGER.debug("服务器启动成功");
        shutdown = false;
        return;
    }

    @Override
    public synchronized void shutdown() {
        if (shutdown) {
            LOGGER.debug("服务器已经关闭，请勿重复关闭");
            return;
        }

        LOGGER.info("销毁MVC数据处理器");
        LOGGER.debug("关闭正在运行的数据处理，等待当前数据处理的完成");
        service.shutdown();
        LOGGER.debug("数据处理完成，关闭数据处理服务");

        beanContainer.shutdown();
        sessionManager.shutdown();

        resourceContainer.shutdown();
        filterContainer.shutdown();
        dataWriterContainer.shutdown();
        dataReaderContainer.shutdown();
        exceptionMapperContainer.shutdown();
        paramParserContainer.shutdown();
        LOGGER.info("MVC数据处理器销毁成功");
        shutdown = true;
    }

    @Override
    public <R extends RequestContext> void deal(Datagram datagram, R requestContext, Consumer<InterfaceData>
            consumer) throws NullPointerException {
        LOGGER.debug("读取到数据为：{}", datagram);
        if (datagram == null) {
            LOGGER.warn("接收到的数据为空，不处理");
            throw new NullPointerException("接收到的数据为空");
        }

        if (requestContext == null) {
            LOGGER.warn("RequestContext为空，不处理");
            throw new NullPointerException("RequestContext为空");
        }

        if (consumer == null) {
            LOGGER.warn("Consumer为空");
            throw new NullPointerException("Consumer为空");
        }

        if (worker(datagram.getType())) {
            LOGGER.debug("该数据可以处理，提交到线程池开始处理");
            service.submit(() -> {
                InterfaceData result = accept(datagram.getBody(), requestContext);
                LOGGER.debug("MVC数据处理器处理{}的结果为：{}", datagram, result);
                consumer.accept(result);
            });
        }
    }

    /**
     * 初始化
     */
    private void init() {
        LOGGER.info("开始初始化MVC数据处理器");

        //构建数据处理ThreadFactory
        ThreadFactory factory = new ThreadFactory() {
            AtomicInteger counter = new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                return new Thread(r, String.format(threadName, this.counter.getAndAdd(1)));
            }
        };

        //构建线程池
        LOGGER.debug("构建线程池，参数为{}:{}:{}:{}", minThread, maxThread, threadAliveTime, threadName);
        service = new ThreadPoolExecutor(minThread, maxThread, threadAliveTime, TimeUnit.SECONDS, new
                LinkedBlockingQueue<>(), factory);
        LOGGER.debug("线程池构建完成");


        //不加该行service中没有提交过任务时当程序运行完毕会自动关闭，只有这样该线程组才不会自动关闭，后端项目才不
        // 会启动后就立即自动关闭
        service.submit(() -> LOGGER.debug("线程组启动成功"));

        LOGGER.info("初始化容器");
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
        LOGGER.info("容器初始化完毕");
        LOGGER.info("MVC数据处理器初始化完毕");
    }

    /**
     * 处理数据
     *
     * @param body 待处理数据
     * @param r    RequestContext
     * @param <R>  RequestContext的类型
     * @return 处理结果
     * @throws NullPointerException 此处有可能抛出NullPointException，在message解析失败时会抛出{@link RequestDataError RequestDataError}
     *                              异常，该异常系统不会自动处理，最后会交给用户异常处理器处理，而当用户异常处理器没有正确处理该异常时最终会抛
     *                              出{@link NullPointerException NullPointerException}，主要是由于构建异常消息时需要从message中获取ID，而此时
     *                              message是null，所以会抛出@link NullPointerException NullPointerException}
     */
    private <R extends RequestContext> InterfaceData accept(byte[] body, R r) throws NullPointerException {
        LOGGER.debug("接收到数据，开始处理。[{}]", body);
        HttpResponseContext responseContext;
        InterfaceData resultData;
        InterfaceData message = null;
        HttpRequestContext requestContext = (HttpRequestContext) r;

        if (body == null || body.length == 0) {
            //请求必须有请求体，否则最基本的invoke信息都没有
            LOGGER.warn("该请求没有请求体，请求内容为null");
            return null;
        }
        // 构建响应上下文，必须在此处初始化，否则当发生异常的时候异常中获取到的responseContext是空，无法获取到信息
        responseContext = new HttpResponseContext();
        // MVC数据处理器只有这一种请求data，直接读取
        LOGGER.debug("开始解析请求数据");

        try {
            message = JSON_PARSER.readAsObject(body, InterfaceData.class);
            if (message == null) {
                String msg = Arrays.toString(body);
                LOGGER.debug("数据解析异常，用户数据为：[{}]，要解析的类型为：[{}]", msg, InterfaceData.class);
                throw new RequestDataError("请求数据异常，数据为：" + msg);
            }
            LOGGER.debug("请求数据解析完毕，请求数据为：{}", message);

            // 搜索指定的resource
            Resource resource = findResource(message.getInvoke());
            // 放进请求上下文
            requestContext.setResource(resource);
            LOGGER.debug("请求的资源查找完成，请求资源为：{}", resource);

            // 开始查找数据编码器
            LOGGER.debug("开始查找数据编码器");
            // 找到请求数据编码处理器
            DataReader dataReader = findReaderInterceptor(resource.getConsume());
            requestContext.setReader(dataReader);
            LOGGER.debug("数据编码器为：{}", dataReader);

            LOGGER.debug("开始解码参数");
            // 开始解码数据
            Object[] param = dataReader.read(resource.getParams(), requestContext, message.getData());
            requestContext.setParams(param);
            LOGGER.debug("参数解码完毕，参数为：{}", param);

            LOGGER.debug("开始验证参数");
            resource.check(param);
            LOGGER.debug("参数验证完毕");

            LOGGER.debug("开始请求filter");
            // 请求filter
            filterContainer.requestFilter(requestContext.getRequest());
            LOGGER.debug("filter完毕，开始调用资源");

            // 调用资源
            Object result = resource.invoke(requestContext);
            responseContext.getResponse().setResult(result);
            LOGGER.debug("资源调用完毕，请求结果为：{}", result);

            // 响应
            LOGGER.debug("开始处理响应");
            resultData = response(requestContext, responseContext, message);
            LOGGER.debug("响应处理完毕");
        } catch (ResourceNotFoundException e) {
            LOGGER.error("用户请求的资源不存在", e);
            resultData = buildResult(new BaseDTO<>("404"), message.getId(), message.getInvoke(),
                    findWriterInterceptor(null));
        } catch (MediaTypeNoSupportException e) {
            LOGGER.error("找不到对应的参数解析器", e);
            resultData = buildResult(new BaseDTO<>("505"), message.getId(), message.getInvoke(),
                    resolveDataInterceptor(requestContext, responseContext));
        } catch (ParamParserException e) {
            LOGGER.error("参数解析失败", e);
            resultData = buildResult(BaseDTO.buildError("400"), message.getId(), message.getInvoke(),
                    resolveDataInterceptor(requestContext, responseContext));
        } catch (ValidationException e) {
            LOGGER.error("参数验证失败", e);
            resultData = buildResult(BaseDTO.buildError("505"), message.getId(), message.getInvoke(),
                    resolveDataInterceptor(requestContext, responseContext));
        } catch (FilterException e) {
            LOGGER.error("filter异常");
            resultData = buildResult(BaseDTO.buildError(), message.getId(), message.getInvoke(),
                    resolveDataInterceptor(requestContext, responseContext));
        } catch (Throwable e) {
            // 请求过程中发生了异常
            LOGGER.error("请求过程中发生了异常，开始查找相应的异常处理器处理异常", e);

            // 查找异常处理器
            List<ExceptionMapper> exceptionMappers = exceptionMapperContainer.select(mapper -> mapper.mapper(e));

            LOGGER.info("异常处理器查找完毕");
            if (exceptionMappers.isEmpty()) {
                LOGGER.error("异常没有找到相应的处理器", e);
                resultData = buildResult(BaseDTO.buildError(), message.getId(), message.getInvoke(),
                        resolveDataInterceptor(requestContext, responseContext));
            } else {
                LOGGER.info("找到异常处理器，由相应的异常处理器处理");
                try {
                    HttpResponseContext.Response response = exceptionMappers.get(0).toResponse(e);
                    resultData = new InterfaceData(message.getId(), message.getInvoke(), resolveDataInterceptor
                            (requestContext, responseContext).write(response.getResult()));
                } catch (Throwable throwable) {
                    LOGGER.error("用户异常处理器[{}]处理异常[{}]的过程中又发生了异常", exceptionMappers.get(0), e, throwable);
                    resultData = buildResult(BaseDTO.buildError(), message.getId(), message.getInvoke(),
                            resolveDataInterceptor(requestContext, responseContext));
                }
            }
        }
        return resultData;
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
    private InterfaceData response(HttpRequestContext requestContext, HttpResponseContext responseContext, InterfaceData
            userData) throws MediaTypeNoSupportException, FilterException {
        LOGGER.debug("开始构建响应");
        HttpRequestContext.RequestWrapper request = requestContext.getRequest();
        Resource resource = requestContext.getResource();
        HttpResponseContext.Response response = responseContext.getResponse();

        // 请求过程中没有发生异常
        // 响应filter
        filterContainer.responseFilter(request, response);

        // 找到处理响应的编码器
        DataWriter dataWriter = findWriterInterceptor(resource.getProduce());
        responseContext.setWriter(dataWriter);

        Object result = response.getResult();

        LOGGER.debug("请求结果为：{}", result);

        // 根据不同的结果分别处理，资源有可能没有返回，有可能返回聊天消息，也有可能返回正常的数据
        if (result == null) {
            LOGGER.debug("请求的接口{}没有响应消息，返回一个BaseDTO", userData.getInvoke());
            // 如果该接口没有响应消息，那么返回一个基本的请求成功
            return buildResult(BaseDTO.buildSuccess(), userData.getId(), userData.getInvoke(),
                    dataWriter);
        } else if (result instanceof InterfaceData) {
            LOGGER.debug("用户响应的信息是InterfaceData对象");
            return (InterfaceData) result;
        } else {
            LOGGER.debug("请求接口{}的响应信息为：{}", userData.getInvoke(), result);
            // 该接口有响应消息并且不是聊天类型消息，那么直接将该消息返回
            return buildResult(result, userData.getId(), userData.getInvoke(), dataWriter);
        }
    }

    /**
     * 构建响应结果
     *
     * @param result     响应的结果
     * @param id         对应的请求消息的ID
     * @param invoke     对应的请求消息请求的接口名
     * @param dataWriter 对应的数据处理器
     * @return 响应结果
     */
    private InterfaceData buildResult(Object result, String id, String invoke, DataWriter
            dataWriter) {
        invoke = invoke.startsWith("/") ? "/back" + invoke : "/back/" + invoke;
        if (dataWriter == null) {
            return null;
        }
        return new InterfaceData(id, invoke, dataWriter.write(result));
    }

    /**
     * 查找数据解码器
     *
     * @param consume 待读取内容的类型
     * @return 指定类型对应的数据解码器
     * @throws MediaTypeNoSupportException 找不到指定类型数据解码器时抛出该异常
     */
    private DataReader findReaderInterceptor(String consume) throws MediaTypeNoSupportException {
        LOGGER.debug("要查找的数据解码器类型为：{}", consume);
        final String realConsume = StringUtils.isEmpty(consume) ? "json" : consume;
        List<DataReader> dataReaders = dataReaderContainer.select(reader -> reader.isReadable
                (realConsume));

        LOGGER.debug("数据编码器为：{}", dataReaders);
        if (dataReaders.isEmpty()) {
            // 找不到支持
            throw new MediaTypeNoSupportException(consume);
        }

        return dataReaders.get(0);
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
            LOGGER.error("请求信息中没有请求资源的名字");
            throw new ResourceNotFoundException(String.valueOf(path));
        }
        Resource resource = resourceContainer.findResource(path);
        LOGGER.debug("请求的资源为：{}", resource);

        if (resource == null) {
            // 找不到要请求的resource
            LOGGER.error("没有找到{}对应的资源，请检查请求地址是否有误", path);
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
    private DataWriter findWriterInterceptor(String produce) throws MediaTypeNoSupportException {
        LOGGER.debug("查找{}格式的数据编码器", produce);
        final String dataProduce = StringUtils.isEmpty(produce) ? "json" : produce;

        List<DataWriter> dataWriters = dataWriterContainer.select(dataInterceptor -> dataInterceptor
                .isWriteable(dataProduce));

        if (dataWriters.isEmpty()) {
            // 找不到支持
            throw new MediaTypeNoSupportException(String.valueOf(dataProduce));
        }
        return dataWriters.get(0);
    }

    /**
     * 确定一个响应数据处理器
     *
     * @param requestContext  请求上下文
     * @param responseContext 响应上下文
     * @return 响应数据处理器，该方法肯定会返回一个响应数据处理器
     */
    private DataWriter resolveDataInterceptor(HttpRequestContext requestContext, HttpResponseContext
            responseContext) {
        LOGGER.debug("根据上下文确定一个响应数据处理器");

        DataWriter writer = responseContext.getWriter();

        if (writer != null) {
            LOGGER.debug("响应过程中已经确定了响应数据处理器，直接返回");
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

    /**
     * 该MVC处理器是否可以处理指定类型的数据
     *
     * @param type 数据类型
     * @return 返回true表示可以处理
     */
    private boolean worker(int type) {
        return type == 1;
    }

    static {
        // 参数错误
        BaseDTO.addStatus("400", "Fail Param");
        // 找不到参数解析器
        BaseDTO.addStatus("505", "Data Parser Not Found");
        // 找不到指定资源
        BaseDTO.addStatus("404", "NotFoundResource");
        // 数据非法
        BaseDTO.addStatus("501", "DataError");
        // 已经有其他用户登录
        BaseDTO.addStatus("502", "Other User Login");
        // 密码修改，请重新登录
        BaseDTO.addStatus("503", "Password Has Changed");
        // 心跳超时
        BaseDTO.addStatus("504", "Timeout");
        // 参数验证失败
        BaseDTO.addStatus("505", "validation fail");
    }
}
