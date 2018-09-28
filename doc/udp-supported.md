## UDP支持
最新的版本的支持UDP，但是UDP功能当前只是在测试开发中。使用UDP功能只需要在配置的时候添加如下代码：
```
com.joe.easysocket.server.balance.Config config = com.joe.easysocket.server.balance.Config.builder()
                    .connectorManager(UDPConnectorManager.class.getName()).clusterConfig(ClusterConfig.builder()
                            .publishCenter(publishCenter).registry(registry).build()).port(10051).host(host).build();
```
可以看出与TCP不同的是，使用UDP需要在配置指定使用UDPConnectorManager，这样就能开启UDP了，底层将默认使用UDP而不是TCP。

**注意，当前UDP支持还在开发测试中，并且没有相应的客户端，需要用户自己编写客户端，规则与TCP相同**