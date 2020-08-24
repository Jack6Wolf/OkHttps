package com.jack.okhttpdemo;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    OkHttpClient okHttpClient;
    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    /**
     * Android中检测是否存在代理的核心代码
     */
    public static boolean detectIfProxyExist(Context ctx) {
        boolean IS_ICS_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
        String proxyHost;
        int proxyPort;
        try {
            if (IS_ICS_OR_LATER) {
                proxyHost = System.getProperty("http.proxyHost");
                String port = System.getProperty("http.proxyPort");
                proxyPort = Integer.parseInt(port != null ? port : "-1");
            } else {
                proxyHost = android.net.Proxy.getHost(ctx);
                proxyPort = android.net.Proxy.getPort(ctx);

            }
            return proxyHost != null && proxyPort != -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        builder.eventListenerFactory(MyOkHttpEventListener.FACTORY);
        builder.addInterceptor(new HttpDnsInterceptor(this));
        setSsl();
        okHttpClient = builder.build();
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
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                if (hostname.contains("getman.cn"))
                    return true;
                else
                    return false;
            }
        });
        builder.sslSocketFactory(sslContext.getSocketFactory(), trustManagers[0]);
    }

    private void request(String url, OkHttpClient okHttpClient) {
        Request.Builder builder = new Request.Builder();
        //必须携带该请求头
        builder.header("host", "getman.cn");
        final Request request = builder
                .url(url)
                .get()//默认就是GET请求，可以不写
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "onResponse: " + response.body().string());
            }
        });
    }

    public void click(View view) {
        request("https://getman.cn/mock/test/jack", okHttpClient);
    }

    public void click1(View view) {
        request("https://27.221.54.228/mock/test/jack", okHttpClient);
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