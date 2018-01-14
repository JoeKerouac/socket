#项目说明
本项目是easysocket项目的升级版，在原有的单机基础上集成了分布式支持，项目分为五部分，分别是：
-balance：balance项目为前端项目，运行在服务器，负责接收、处理、保持客户端连接和往后端服务器分发客户端的数据。
-backserver：backserver项目为后端服务器项目，运行在服务器，负责处理前端分发过来的客户端数据。
-common：common项目为前端项目和后端项目的通用部分。
-client：client项目为客户端参考实现，用于测试使用，不建议直接用于生产环境。
-socket-test：socket-test项目使用balance项目和backserver项目实现了一个简易的socket服务器，用于给用户提供使用参考。
