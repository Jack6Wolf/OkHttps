package com.star.test;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CertificateTest {
    private OkHttpClient okHttpClient;
    private OkHttpClient.Builder builder = new OkHttpClient.Builder();


    public CertificateTest() {
        setSsl();
        okHttpClient = builder.build();
    }

    public static void main(String[] args) {
//        new CertificateTest().request("https://getman.cn/mock/test/jack");
//        new CertificateTest().request("https://183.201.241.79/mock/test/jack");
        new CertificateTest().request("https://39.156.66.18");
    }

    private void setSsl() {
        X509TrustManager[] trustManagers = new X509TrustManager[]{new TrustAllManager()};
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, trustManagers, new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        builder.sslSocketFactory(sslContext.getSocketFactory(), trustManagers[0]);
//        builder.hostnameVerifier(new HostnameVerifier() {
//            @Override
//            public boolean verify(String hostname, SSLSession session) {
//                if (hostname.contains("getman.cn"))
//                    return true;
//                else
//                    return false;
//            }
//        });
    }

    private void request(String url) {
        Request.Builder builder = new Request.Builder();
        //必须携带该请求头
        builder.header("host", "www.baidu.com");
        final Request request = builder
                .url(url)
                .get() //默认就是GET请求
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.exit(0);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("onResponse: " + response.body().string());
                System.exit(0);
            }
        });
    }


    public static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                chain[0].checkValidity();
            } catch (Exception e) {
                throw new CertificateException("Certificate not valid or trusted.");
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

}
