package com.joe.easysocket.server.backserver.mvc.impl.context;

import com.joe.easysocket.server.backserver.mvc.coder.DataWriter;
import com.joe.easysocket.server.backserver.mvc.context.ResponseContext;
import com.joe.easysocket.server.backserver.mvc.data.BaseDTO;
import lombok.Data;

@Data
public class HttpResponseContext implements ResponseContext{
    // 响应对象
    private Response response;
    // 响应数据编码器
    private DataWriter writer;

    public HttpResponseContext() {
        this.response = new Response();
    }

    @Data
    public static class Response {
        /*
         * 请求结果
         */
        private Object result;

        private Response() {

        }

        /**
         * 构建一个无数据的简单响应
         *
         * @return 没有业务数据的简单响应（result是一个BaseDTO）
         */
        public static Response buildOk() {
            Response response = new Response();
            response.setResult(BaseDTO.buildSuccess());
            return response;
        }

        /**
         * 构建一个系统异常响应
         *
         * @return 系统异常响应
         */
        public static Response buildError() {
            Response response = new Response();
            response.setResult(BaseDTO.buildError());
            return response;
        }

        /**
         * 构建一个自定义响应
         *
         * @param result 响应结果
         * @return 自定义响应
         */
        public static Response build(Object result) {
            Response response = new Response();
            response.setResult(result);
            return response;
        }
    }
}
