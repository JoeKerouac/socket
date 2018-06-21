# 编解码规范
## 名词解释
- 数据报：本程序的底层通讯消息最小单位，本程序中底层通讯是通过数据报来进行的；
- header(报头,数据报请求头）：详细解释参见数据报定义；
- body（数据报请求体）：详细定义参见数据报定义；
## 数据报定义
一个数据报为一个byte数组，分为header和body，其中header为固定的前56byte，第一个byte为版本号，第二到第五4byte为数据报长度，第六个字节为数据报数据类型，第七到第十六字节为数据报编码，第十七到五十六字节为ID，然后后边放实际数据，详细说明如下：
- 长度数据说明，第二道第五4byte数据为一个32位有符号int类型数据转换而来，将int按照8位大小拆分，高位在前，低位在后构建为4个byte数据，该长度为数据报body的长度，不包含header；
- 类型说明：第六个字节为数据报类型，类型如下：0：心跳包；1：内置MVC数据处理器数据类型；2：文件传输（保留，暂时无用）；3：ACK；4：后端主动发往前端的数据；
- 编码：第七到第十六字节为编码信息，例如UTF8等，该编码信息填充规则如下：获取改编码字符串的byte数组，如果该数组长度小于10那么在后边补0填充到10位长度；
- ID：第十七到第五十六字节为ID，同一客户端ID必须是短时间内唯一的；
## 编解码规范
### 编码
构建流程：
1. 获取实际要发送的数据，将数据转换为byte数组（记录下该转换使用的编码charset）；
2. 获取byte数组的长度len（有符号int类型）；
3. 开始构建请求头，首先放入版本号；
4. 将长度len按照上述说明转换为4byte数据放入请求头；
5. 将数据报的类型放入请求头；
6. 将前边的charset按照定义中转换为byte数组放入请求头；
7. 生成一个ID（该ID需要在转换为byte数组后长度为40，同时需要符合数据报定义）；
8. 将请求头和实际要发送的数据拼接成一个完整的数据报。
### 解码
按照编码规则逆运算即可解码。
### 参考
程序中提供了[数据报的封装对象com.joe.easysocket.server.common.data.Datagram](common/src/main/java/com/joe/easysocket/server/common/data/Datagram.java))方便对数据报进行操作，同时提供了[数据报工具类com.joe.easysocket.server.common.data.DatagramUtil](common/src/main/java/com/joe/easysocket/server/common/data/DatagramUtil.java))来快速构[数据报的封装对象](common/src/main/java/com/joe/easysocket/server/common/data/Datagram.java))，客户端也可参考这两个类来实现数据报的编码解码。

# 客户端与服务器通信模型（MVC）
当服务器提供MVC类型数据的后端，需要客户端使用指定数据模型（数据报的请求体）通信，通信模型如下：
```
{
    "id" : "该消息ID，需要确保短时间内是唯一的（客户端生成，响应的时候将该ID返回去），Stirng类型",
    "invoke" : "调用的接口，例如/say，Stirng类型",
    "data" : "要发送的数据，Stirng类型"
}
```