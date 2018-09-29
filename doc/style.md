## 日志说明
代码中打印日志的地方，如果日志的参数部分是由函数得来，并且该函数较复杂，那么需要现在外层判断日志是否打印，否则即使日志不打印的时候该函数也会被调用。

示例代码：
```
// 常量可以不用判断
log.debug("count is {}" , 1);

//要打印的是一个函数的结果，需要加上判断
byte[] body = new byte[1024];
if (log.isDebugEnabled()) {
    log.debug("要发送的数据为：{}", Arrays.toString(body));
}
```