package com.joe.test.easysocket.client;

import com.joe.easysocket.client.data.InterfaceData;
import com.joe.easysocket.client.ext.EventListenerAdapter;
import com.joe.easysocket.client.ext.Logger;
import com.joe.easysocket.client.ext.Serializer;
import com.joe.easysocket.client.Client;
import com.joe.easysocket.client.core.EventListener;
import com.joe.utils.concurrent.ThreadUtil;
import com.joe.utils.parse.json.JsonObject;
import com.joe.utils.parse.json.JsonParser;


/**
 * @author joe
 */
public class ClientTest {
    private static final JsonParser parser = JsonParser.getInstance();

    public static void main(String[] args) throws Exception {
//        byte[] data = {1, 0, 0, 0, 54, 4, 85, 84, 70, 45, 56, 0, 0, 0, 0, 0, 51, 100, 57, 97, 101, 57, 99, 100, 49,
// 97, 97, 99, 52, 51, 98, 56, 56, 100, 54, 51, 52, 99, 97, 97, 48, 100, 56, 102, 100, 54, 57, 101, 0, 0, 0, 0, 0, 0,
// 0, 0, -28, -67, -96, -27, -91, -67, -27, -107, -118, -17, -68, -116, -24, -65, -103, -26, -104, -81, -28, -72,
// -128, -26, -99, -95, -28, -72, -69, -27, -118, -88, -27, -113, -111, -27, -66, -128, -27, -82, -94, -26, -120,
// -73, -25, -85, -81, -25, -102, -124, -26, -74, -120, -26, -127, -81};
//        System.out.println(new String(data));
//        System.exit(0);
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

        EventListener listener = new EventListenerAdapter() {
            @Override
            public void faild(Throwable cause) {
                System.out.println("faild");
            }

            @Override
            public void register(Client client) {
                System.out.println("register");
            }

            @Override
            public void reconnect(Client client) {
                System.out.println("reconnect");
            }

            @Override
            public void unregister() {
                System.out.println("unregister");
            }

            @Override
            public void receive(InterfaceData data) {
                System.out.println("receive:" + data);
            }
        };

        //构建client对象，其中logger和listener都是非必须的，但是没有listener就无法处理服务器消息，所以正式使用时该对象必须有
        Client client = Client.builder().heartbeat(30).host("127.0.0.1").port(10051).serializer(serializer).listener
                (listener).logger(logger).build();
        client.start();
        JsonObject object = new JsonObject().data("account", 123).data("password", "345");
        client.write("user/login", object.toJson());

        ThreadUtil.sleep(15);
        for (int i = 0; i < 100000; i++) {
            Thread.sleep(3000);
            client.write("user/print", null);
        }
        System.out.println("完成");
    }
}
