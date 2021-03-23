package com.star.test;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author jack
 * @since 2020/8/21 17:12
 */
public class HttpDnsInterceptor implements Interceptor {


    @Override
    public Response intercept(Chain chain) throws IOException {
        try {
            return chain.proceed(chain.request());
        } catch (IOException e) {
            //域名解析失败
            if (NetworkUtils.isConnect()) {
                Request originRequest = chain.request();
                Request.Builder builder = originRequest.newBuilder();
                Request newRequest = builder.url("https://39.156.66.18")
                        //必须携带该请求头
                        .header("host", "www.baidu.com")
                        .build();
                return chain.proceed(newRequest);
            } else {
                throw e;
            }

        }
    }
}
