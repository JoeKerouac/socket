package com.joe.easysocket.server.common.registry;

/**
 * @author joe
 */
public enum ConnectionState {

    /**
     * 客户端连接成功
     */
    CONNECTED {
        public boolean isConnected() {
            return true;
        }
    },


    /**
     * 客户端被挂起
     */
    SUSPENDED {
        public boolean isConnected() {
            return false;
        }
    },

    /**
     * 客户端重连
     */
    RECONNECTED {
        public boolean isConnected() {
            return true;
        }
    },

    /**
     * 客户端连接丢失
     */
    LOST {
        public boolean isConnected() {
            return false;
        }
    },

    /**
     * 客户端被设置为只读状态
     */
    READ_ONLY {
        public boolean isConnected() {
            return true;
        }
    };

    /**
     * 检查当前是否连接到了服务器
     *
     * @return 已经连接到服务器返回true，否则返回false
     */
    public abstract boolean isConnected();
}
