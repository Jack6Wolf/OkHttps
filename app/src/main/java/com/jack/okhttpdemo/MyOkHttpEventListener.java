package com.jack.okhttpdemo;


import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 接收一系列网络请求过程中的事件，例如DNS解析、TSL/SSL连接、Response接收等。
 * 通过继承此接口，调用者可以监视整个应用中网络请求次数、流量大小、耗时情况。
 *
 * @author jack
 * @since 2019/9/11 16:33
 */
public class MyOkHttpEventListener extends EventListener {
    /**
     * 自定义EventListener工厂
     */
    public static final Factory FACTORY = new Factory() {
        final AtomicLong nextCallId = new AtomicLong(1L);

        @Override
        public EventListener create(Call call) {
            long callId = nextCallId.getAndIncrement();
            return new MyOkHttpEventListener(callId, call.request().url(), System.nanoTime());
        }
    };
    private static final String TAG = "MyOkHttpEventListener";
    /**
     * 每次请求的标识
     */
    private final long callId;
    /**
     * 每次请求的开始时间，单位纳秒
     */
    private final long callStartNanos;
    /**
     * 每次dns的开始时间，单位纳秒
     */
    private long dnsStartNanos;
    private StringBuilder sbLog;

    public MyOkHttpEventListener(long callId, HttpUrl url, long callStartNanos) {
        this.callId = callId;
        this.callStartNanos = callStartNanos;
        this.sbLog = new StringBuilder(url.toString()).append(" ").append(callId).append(":\n");
    }

    private void recordEventLog(String name) {
        long elapseNanos = System.nanoTime() - callStartNanos;
        sbLog.append(String.format(Locale.CHINA, "%.3f-%s", elapseNanos / 1000000000d, name)).append(";\n");
        if (name.equalsIgnoreCase("callEnd") || name.equalsIgnoreCase("callFailed")) {
            //打印出每个步骤的时间点
            Log.d(TAG, sbLog.toString());
        }
    }

    /**
     * 请求开始
     * 当一个Call（代表一个请求）被同步执行或被添加异步队列中时。
     * 由于线程或事件流的限制，这里的请求开始并不是真正的去执行的这个请求。
     * 如果发生重定向和多域名重试时，这个方法也仅被调用一次。
     */
    @Override
    public void callStart(Call call) {
        super.callStart(call);
        recordEventLog("callStart");
    }

    /**
     * dnsStart/dnsEnd dns解析开始/结束
     */
    @Override
    public void dnsStart(Call call, String domainName) {
        super.dnsStart(call, domainName);
        recordEventLog("dnsStart:" + domainName);

    }

    /**
     * dnsStart/dnsEnd dns解析开始/结束
     */
    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        super.dnsEnd(call, domainName, inetAddressList);
        StringBuilder stringBuilder = new StringBuilder();
        for (InetAddress inetAddress : inetAddressList) {
            stringBuilder.append(inetAddress.getHostAddress()).append(",");
        }
        recordEventLog("dnsEnd:" + domainName+":"+stringBuilder.toString());

    }

    /**
     * connectStart/connectEnd 连接开始结束
     * OkHttp是使用Socket接口建立Tcp连接的，所以这里的连接就是指Socket建立一个连接的过程。
     * 当连接被重用时，connectStart/connectEnd不会被调用。
     * 当请求被重定向到新的域名后，connectStart/connectEnd会被调用多次。
     */
    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        super.connectStart(call, inetSocketAddress, proxy);
        recordEventLog("connectStart");
    }

    /**
     * TLS安全连接开始和结束
     * 当存在重定向或连接重试的情况下，secureConnectStart/secureConnectEnd会被调用多次。
     */
    @Override
    public void secureConnectStart(Call call) {
        super.secureConnectStart(call);
        recordEventLog("secureConnectStart");
    }

    @Override
    public void secureConnectEnd(Call call, @Nullable Handshake handshake) {
        super.secureConnectEnd(call, handshake);
        recordEventLog("secureConnectEnd");
    }

    /**
     * 使用HTTPS安全连接，在TCP连接成功后需要进行TLS安全协议通信，
     * 等TLS通讯结束后才能算是整个连接过程的结束，
     * 也就是说connectEnd在secureConnectEnd之后调用。
     * 在连接过程中，无论是Socket连接失败，还是TSL/SSL握手失败，都会回调connectEnd。
     */
    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, @Nullable Protocol protocol) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol);
        recordEventLog("connectEnd");
    }


    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, @Nullable Protocol protocol, IOException ioe) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
        recordEventLog("connectFailed");
    }

    /**
     * connectionAcquired是在连接成功后被调用的。但是在连接复用的情况下没有连接步骤，connectAcquired会在获取缓存连接后被调用。
     * 如果直接复用StreamAllocation中的连接，则不会调用connectionAcquired/connectReleased。
     * 找到合适的连接后，会在基于当前连接构建Http的编解码器HttpCodec，来解析Http请求和响应。
     */
    @Override
    public void connectionAcquired(Call call, Connection connection) {
        super.connectionAcquired(call, connection);
        recordEventLog("connectionAcquired");

    }

    /**
     * 当一个流被主动关闭或异常关闭时，就需要把这个流对应的资源释放(deallocate)掉。
     * 资源释放的两个方面：
     * 1. 将StreamAllocation的引用从RealConnection的队列中移除掉
     * 2. 将RealConnection在连接池中变成空闲状态
     */
    @Override
    public void connectionReleased(Call call, Connection connection) {
        super.connectionReleased(call, connection);
        recordEventLog("connectionReleased");
    }

    /**
     * 发送请求头
     */
    @Override
    public void requestHeadersStart(Call call) {
        super.requestHeadersStart(call);
        recordEventLog("requestHeadersStart");
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        super.requestHeadersEnd(call, request);
        recordEventLog("requestHeadersEnd");
    }

    /**
     * 发送请求体
     */
    @Override
    public void requestBodyStart(Call call) {
        super.requestBodyStart(call);
        recordEventLog("requestBodyStart");
    }

    @Override
    public void requestBodyEnd(Call call, long byteCount) {
        super.requestBodyEnd(call, byteCount);
        recordEventLog("requestBodyEnd");
    }

    /**
     * 读取响应头
     */
    @Override
    public void responseHeadersStart(Call call) {
        super.responseHeadersStart(call);
        recordEventLog("responseHeadersStart");
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        super.responseHeadersEnd(call, response);
        recordEventLog("responseHeadersEnd");
    }

    /**
     * 读取响应体
     */
    @Override
    public void responseBodyStart(Call call) {
        super.responseBodyStart(call);
        recordEventLog("responseBodyStart");
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        super.responseBodyEnd(call, byteCount);
        recordEventLog("responseBodyEnd");
    }

    /**
     * callEnd也有两种调用场景。
     * 第一种也是在关闭流时。
     * 第二种是在释放连接时。
     */
    @Override
    public void callEnd(Call call) {
        super.callEnd(call);
        recordEventLog("callEnd");
    }

    /**
     * callFailed在两种情况下被调用
     * 第一种是在请求执行的过程中发生异常时。
     * 第二种是在请求结束后，关闭输入流时产生异常时。
     */
    @Override
    public void callFailed(Call call, IOException ioe) {
        super.callFailed(call, ioe);
        recordEventLog("callFailed");
    }
}

