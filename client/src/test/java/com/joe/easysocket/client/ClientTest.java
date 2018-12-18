package com.joe.easysocket.client;

import com.joe.easysocket.client.core.EventListener;
import com.joe.easysocket.client.data.InterfaceData;
import com.joe.easysocket.client.ext.Logger;
import com.joe.easysocket.client.ext.MessageListener;
import com.joe.easysocket.client.ext.Serializer;
import com.joe.utils.concurrent.ThreadUtil;
import com.joe.utils.serialize.json.JsonObject;
import com.joe.utils.serialize.json.JsonParser;

/**
 * @author joe
 */
public class ClientTest {
    private static final JsonParser parser = JsonParser.getInstance();

    public static void main(String[] args) throws Exception {
        Serializer serializer = new Serializer() {
            @Override
            public byte[] write(Object obj) {
                if (obj instanceof byte[]) {
                    return (byte[]) obj;
                } else if (obj instanceof String) {
                    return ((String) obj).getBytes();
                }
                return parser.toJson(obj).getBytes();
            }

            @Override
            public <T> T read(byte[] data, Class<T> clazz) {
                return parser.readAsObject(data, clazz);
            }
        };

        //日志
        Logger logger = new Logger() {
            @Override
            public void debug(String msg) {
                System.out.println(msg);
            }

            @Override
            public void info(String msg) {
                System.out.println(msg);
            }

            @Override
            public void warn(String msg) {
                System.out.println(msg);
            }

            @Override
            public void error(String msg) {
                System.out.println(msg);
            }

            @Override
            public void debug(String flag, String msg) {
                System.out.println(flag + ":" + msg);
            }

            @Override
            public void info(String flag, String msg) {
                System.out.println(flag + ":" + msg);
            }

            @Override
            public void warn(String flag, String msg) {
                System.out.println(flag + ":" + msg);
            }

            @Override
            public void error(String flag, String msg) {
                System.out.println(flag + ":" + msg);
            }
        };

        EventListener listener = new MessageListener() {
            @Override
            public void receive(InterfaceData data) {
                System.out.println("数据是：" + data);
            }

            @Override
            public Serializer getSerializer() {
                return serializer;
            }
        };

        //构建client对象，其中logger和listener都是非必须的，但是没有listener就无法处理服务器消息，所以正式使用时该对象必须有
        Client client = Client.builder().heartbeat(30).host("127.0.0.1").port(10051)
            .serializer(serializer).logger(logger).build();
        client.register(listener);
        client.start();
        JsonObject object = new JsonObject().data("account", 123).data("password", "345");

        //调用登录方法
        client.write("user/login", object.toJson());

        //调用服务端的打印方法
        ThreadUtil.sleep(15);
        for (int i = 0; i < 3; i++) {
            Thread.sleep(3000);
            client.write("user/print", null);
        }
    }
}
