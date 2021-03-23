package com.star.test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author jack
 * @since 2021/3/16 15:41
 */
class CacheTest {
    private final OkHttpClient client;

    public CacheTest(File cacheDirectory) throws Exception {
        System.out.println(String.format("Cache file path %s", cacheDirectory.getAbsoluteFile()));
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(cacheDirectory, cacheSize);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cache(cache);
        builder.networkInterceptors().add(new CacheNetworkInterceptor());
        builder.addInterceptor(new CacheInterceptor());
        client = builder.build();
    }

    public static void main(String... args) throws Exception {
        new CacheTest(new File("D:/CacheResponse.tmp")).run1();
    }

    public void run() throws Exception {
        Request request = new Request.Builder()
                .url("http://publicobject.com/helloworld.txt")
//                .url("https://getman.cn/mock/test/jack")
                .build();

        Response response1 = client.newCall(request).execute();
        if (!response1.isSuccessful()) throw new IOException("Unexpected code " + response1);

        String response1Body = response1.body().string();
        System.out.println("Response 1 response:          " + response1);
        System.out.println("Response 1 cache response:    " + response1.cacheResponse());
        System.out.println("Response 1 network response:  " + response1.networkResponse());

        Response response2 = client.newCall(request).execute();
        if (!response2.isSuccessful()) throw new IOException("Unexpected code " + response2);

        String response2Body = response2.body().string();
        System.out.println("Response 2 response:          " + response2);
        System.out.println("Response 2 cache response:    " + response2.cacheResponse());
        System.out.println("Response 2 network response:  " + response2.networkResponse());

        System.out.println("Response 2 equals Response 1? " + response1Body.equals(response2Body));
    }

    public void run1() {
        Request request = new Request.Builder()
                .url("http://publicobject.com/helloworld.txt")
//                .url("https://getman.cn/mock/test/jack")
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
                System.out.println("Response 1 cache response:    " + response.cacheResponse());
                System.out.println("Response 1 network response:  " + response.networkResponse());
                System.exit(0);
            }
        });
    }

    private static class CacheNetworkInterceptor implements Interceptor {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            //无缓存,进行缓存
            return chain.proceed(chain.request()).newBuilder()
                    //对请求进行最大160秒的缓存
                    .addHeader("Cache-Control", "max-age=160")
                    .build();
        }
    }

    private static class CacheInterceptor implements Interceptor {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Response resp;
            Request req;
            if (NetworkUtils.isConnect()) {
                //有网络,检查60秒内的缓存
                req = chain.request()
                        .newBuilder()
                        .cacheControl(new CacheControl
                                .Builder()
                                .maxAge(60, TimeUnit.SECONDS)
                                .build())
                        .build();
            } else {
                //无网络,检查30天内的缓存,即使是过期的缓存
                req = chain.request().newBuilder()
                        .cacheControl(new CacheControl.Builder()
                                .onlyIfCached()
                                .maxStale(30, TimeUnit.DAYS)
                                .build())
                        .build();
            }
            resp = chain.proceed(req);
            return resp.newBuilder().build();
        }
    }

}
