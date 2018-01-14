package com.joe.easysocket.server.common.protocol;

/**
 * 协议栈异步结果
 *
 * @author joe
 */
public interface ProtocolFuture {
    //失败future
    ProtocolFuture ERRORFUTURE = new ProtocolFuture() {
        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }
    };
    //成功future
    ProtocolFuture SUCCESS = new ProtocolFuture() {
        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }
    };
    /**
     * 是否完成（立即返回）
     *
     * @return 如果完成则返回<code>true</code>
     */
    boolean isDone();

    /**
     * 是否成功（立即返回）
     *
     * @return 如果成功则返回<code>true</code>
     */
    boolean isSuccess();
}
