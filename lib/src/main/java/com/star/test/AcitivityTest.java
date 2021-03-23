package com.star.test;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author jack
 * @since 2021/3/17 12:08
 */
class AcitivityTest {
    private final OkHttpClient client;

    public AcitivityTest() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
//        int cacheSize = 10 * 1024 * 1024; // 10 MiB
//        Cache cache = new Cache(new File("D:/CacheResponse.tmp"), cacheSize);
//        builder.cache(cache);
        client = builder.build();
    }

    public static void main(String[] args) {
        new AcitivityTest().run();
    }

    public void run() {
        Request request = new Request.Builder()
//                .url("https://getman.cn/mock/test/jack")
                .url("http://publicobject.com/helloworld.txt")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.exit(0);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("Response 1 response:          " + response);
                System.exit(0);
            }
        });
    }
}
