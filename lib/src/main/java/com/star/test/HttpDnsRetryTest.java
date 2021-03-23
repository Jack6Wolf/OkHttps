package com.star.test;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author jack
 * @since 2021/3/23 15:19
 */
class HttpDnsRetryTest {
    private final OkHttpClient client;

    public HttpDnsRetryTest() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new HttpDnsInterceptor());
        client = builder.build();
    }

    public static void main(String[] args) {
        //配置代理
        System.setProperty("http.proxyHost", "10.0.63.135");
        System.setProperty("https.proxyHost", "10.0.63.135");
        System.setProperty("http.proxyPort", "8888");
        System.setProperty("https.proxyPort", "8888");
        new HttpDnsRetryTest().run();
    }

    public void run() {
        Request request = new Request.Builder()
                .url("https://jack.jack.com")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.exit(0);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("Response 1 response:          " + response.body().string());
                System.exit(0);
            }
        });
    }
}
