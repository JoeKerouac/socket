package com.joe.easysocket.client.ext;

/**
 * @author joe
 */
public class InternalLogger implements Logger {
    private Logger proxy;
    private String name;

    private InternalLogger(Logger logger, Class clazz) {
        this(logger, clazz.getName());
    }

    private InternalLogger(Logger logger, String name) {
        this.proxy = logger;
        this.name = name;
    }

    /**
     * 根据外部日志实现获取内部日志
     *
     * @param logger 外部日志实现
     * @param clazz  要获取日志的类
     * @return 代理日志
     */
    public static Logger getLogger(Logger logger, Class clazz) {
        return new InternalLogger(logger, clazz);
    }

    /**
     * 根据外部日志实现获取内部日志
     *
     * @param logger 外部日志实现
     * @param name   日志名
     * @return 代理日志
     */
    public static Logger getLogger(Logger logger, String name) {
        return new InternalLogger(logger, name);
    }

    @Override
    public void debug(String msg) {
        proxy.debug(name, msg);
    }

    @Override
    public void info(String msg) {
        proxy.info(name, msg);
    }

    @Override
    public void warn(String msg) {
        proxy.warn(name, msg);
    }

    @Override
    public void error(String msg) {
        proxy.error(name, msg);
    }

    @Override
    public void debug(String flag, String msg) {
        proxy.debug(flag, msg);
    }

    @Override
    public void info(String flag, String msg) {
        proxy.info(flag, msg);
    }

    @Override
    public void warn(String flag, String msg) {
        proxy.warn(flag, msg);
    }

    @Override
    public void error(String flag, String msg) {
        proxy.error(flag, msg);
    }
}
