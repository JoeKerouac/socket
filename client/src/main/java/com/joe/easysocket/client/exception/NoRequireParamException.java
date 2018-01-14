package com.joe.easysocket.client.exception;

/**
 * 缺少必要的参数异常
 *
 * @author joe
 */
public class NoRequireParamException extends SystemException {
    /**
     * 默认构造器
     * @param args
     * 参数数组
     */
    public NoRequireParamException(String... args) {
        super(convert(args));
    }

    private static String convert(String... args){
        String arg = "缺少必要的参数：";
        for (String str : args) {
            if(str == null){
                continue;
            }
            arg += str + "、";
        }
        arg = arg.substring(0 , arg.length() - 1);
        return arg;
    }
}
