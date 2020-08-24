package com.jack.okhttpdemo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author jack
 * @since 2020/8/21 17:12
 */
class HttpDnsInterceptor implements Interceptor {
    private Context context;

    public HttpDnsInterceptor(Context context) {
        this.context = context;
    }

    /**
     * 检测网络是否连接
     */
    public static boolean isNetConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = cm.getAllNetworks();
            NetworkInfo networkInfo;
            for (Network mNetwork : networks) {
                networkInfo = cm.getNetworkInfo(mNetwork);
                if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                    return true;
                }
            }
        } else {
            if (cm != null) {
                NetworkInfo[] infos = cm.getAllNetworkInfo();
                if (infos != null) {
                    for (NetworkInfo ni : infos) {
                        if (ni.isConnected()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        try {
            return chain.proceed(chain.request());
        } catch (IOException e) {
            //域名解析失败
            if (isNetConnected(context)) {
                Request.Builder builder = new Request.Builder();
                Request request = builder.url("https://27.221.54.228/mock/test/jack")
                        //必须携带该请求头
                        .header("host", "getman.cn")
                        .get()//默认就是GET请求，可以不写
                        .build();
                return chain.proceed(request);
            } else {
                throw e;
            }

        }
    }
}
