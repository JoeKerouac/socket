package com.joe.easysocket.client.ext;

/**
 * @author joe
 */
public interface Logger {
    /**
     * debug日志
     *
     * @param msg 日志内容
     */
    void debug(String msg);

    /**
     * info日志
     *
     * @param msg 日志内容
     */
    void info(String msg);

    /**
     * warn日志
     *
     * @param msg 日志内容
     */
    void warn(String msg);

    /**
     * error日志
     *
     * @param msg 日志内容
     */
    void error(String msg);

    /**
     * debug日志
     *
     * @param flag 标志
     * @param msg  日志内容
     */
    void debug(String flag, String msg);

    /**
     * info日志
     *
     * @param flag 标志
     * @param msg  日志内容
     */
    void info(String flag, String msg);

    /**
     * warn日志
     *
     * @param flag 标志
     * @param msg  日志内容
     */
    void warn(String flag, String msg);

    /**
     * error日志
     *
     * @param flag 标志
     * @param msg  日志内容
     */
    void error(String flag, String msg);
}
