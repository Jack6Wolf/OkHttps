package com.star.test;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * OkHttp Auth验证
 *
 * @author jack
 * @since 2021/3/16 11:40
 */
class AuthTest {
    private final OkHttpClient client;

    public AuthTest() {
        client = new OkHttpClient.Builder()
                .authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) {
                        System.out.println("为响应验证：" + response);
                        System.out.println("响应:"+ response.challenges());
                        String credential = Credentials.basic("jesse", "password1");
                        return response.request().newBuilder()
                                .header("Authorization", credential)
                                .build();
                    }
                })
                .build();
    }

    public static void main(String[] args) {
        try {
            new AuthTest().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() throws Exception {
        Request request = new Request.Builder()
                .url("http://publicobject.com/secrets/hellosecret.txt")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("异常:" + response);

        System.out.println("响应:" + response.body().string());
    }


}
