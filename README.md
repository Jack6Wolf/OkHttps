# OkHttp使用HttpDns两种方式
- ip直连方式，即ip+api
- 实现okhttp.dns()中的dns接口

这两种方式各有优缺点，使用Dns接口方式过于底层，异常不容易控制，上层无感知，如果要十分精确的控制异常，
一般使用ip直连方式需要进行两步操作
1. 对url中的host进行替换，将域名替换为ip
2. 添加header请求头，值为替换前的域名

在Http的情况下，这种方式不存在任何问题，但是在Https的情况下，这种方式就会存在诸多问题。
该库基于[okhttp3.12.0](https://github.com/square/okhttp)修改了部分相关代码，解决相关问题。

## Https下使用ip证书校验问题
- okhttp3.internal.connection.RealConnection#connectTls
```java
private void connectTls(ConnectionSpecSelector connectionSpecSelector) throws IOException {
    Address address = route.address();
    SSLSocketFactory sslSocketFactory = address.sslSocketFactory();
    boolean success = false;
    SSLSocket sslSocket = null;
    try {
      // Create the wrapper over the connected socket.
      sslSocket = (SSLSocket) sslSocketFactory.createSocket(
          rawSocket, address.url().host(), address.url().port(), true /* autoClose */);

      // Configure the socket's ciphers, TLS versions, and extensions.
      ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
      if (connectionSpec.supportsTlsExtensions()) {
        Platform.get().configureTlsExtensions(
            sslSocket, address.url().host(), address.protocols());
      }

      // Force handshake. This can throw!
      sslSocket.startHandshake();
      // block for session establishment
      SSLSession sslSocketSession = sslSocket.getSession();
      Handshake unverifiedHandshake = Handshake.get(sslSocketSession);

      // Verify that the socket's certificates are acceptable for the target host.
      if (!address.hostnameVerifier().verify(address.url().host(), sslSocketSession)) {
        X509Certificate cert = (X509Certificate) unverifiedHandshake.peerCertificates().get(0);
        throw new SSLPeerUnverifiedException("Hostname " + address.url().host() + " not verified:"
            + "\n    certificate: " + CertificatePinner.pin(cert)
            + "\n    DN: " + cert.getSubjectDN().getName()
            + "\n    subjectAltNames: " + OkHostnameVerifier.allSubjectAltNames(cert));
      }

      // Check that the certificate pinner is satisfied by the certificates presented.
      address.certificatePinner().check(address.url().host(),
          unverifiedHandshake.peerCertificates());

      // Success! Save the handshake and the ALPN protocol.
      String maybeProtocol = connectionSpec.supportsTlsExtensions()
          ? Platform.get().getSelectedProtocol(sslSocket)
          : null;
      socket = sslSocket;
      source = Okio.buffer(Okio.source(socket));
      sink = Okio.buffer(Okio.sink(socket));
      handshake = unverifiedHandshake;
      protocol = maybeProtocol != null
          ? Protocol.get(maybeProtocol)
          : Protocol.HTTP_1_1;
      success = true;
    } catch (AssertionError e) {
      if (Util.isAndroidGetsocknameError(e)) throw new IOException(e);
      throw e;
    } finally {
      if (sslSocket != null) {
        Platform.get().afterHandshake(sslSocket);
      }
      if (!success) {
        closeQuietly(sslSocket);
      }
    }
  }
```
可以看到，无论是调用`Platform.get().configureTlsExtensions()`配置SSLSocket对象，还是`address.hostnameVerifier().verify()`进行证书校验，以及`address.certificatePinner().check()`中，传入的host都是address.url().host()，而这个值却恰恰是我们替换了url中的域名为ip的host，所以此时拿到的值为ip，这时候，带来了两个问题：
>- 当客户端使用ip直连时，请求URL中的host会被替换成ip，所以在证书验证的时候，会出现domain不匹配的情况，导致SSL/TLS握手不成功。
>- 在服务器上存在多张证书的情况下，会存在问题

而对于服务器上存在多张证书的情况下，为什么会存在问题呢，这里存在一个概念，叫SNI
> SNI（Server Name Indication）是为了解决一个服务器使用多个域名和证书的SSL/TLS扩展。它的工作原理如下：
>- 在连接到服务器建立SSL链接之前先发送要访问站点的域名（Hostname）。
>- 服务器根据这个域名返回一个合适的证书。

目前，大多数操作系统和浏览器都已经很好地支持SNI扩展，OpenSSL 0.9.8也已经内置这一功能。

上述过程中，当客户端使用ip直连时，请求URL中的Host会被替换成IP，导致服务器获取到的域名为解析后的IP，无法找到匹配的证书，只能返回默认的证书或者不返回，所以会出现SSL/TLS握手不成功的错误。

最常见的一个场景就是：

> 比如当你需要通过https访问CDN资源时，CDN的站点往往服务了很多的域名，所以需要通过SNI指定具体的域名证书进行通信。

其实OkHttp是支持SNI的，在Platform.configureTlsExtensions方法中，设置了SNI，只是传入的Host变成了ip，所以导致了这个问题
```java
@Override public void configureTlsExtensions(
      SSLSocket sslSocket, String hostname, List<Protocol> protocols) {
    // Enable SNI and session tickets.
    if (hostname != null) {
      setUseSessionTickets.invokeOptionalWithoutCheckedException(sslSocket, true);
      setHostname.invokeOptionalWithoutCheckedException(sslSocket, hostname);
    }

    // Enable ALPN.
    if (setAlpnProtocols != null && setAlpnProtocols.isSupported(sslSocket)) {
      Object[] parameters = {concatLengthPrefixed(protocols)};
      setAlpnProtocols.invokeWithoutCheckedException(sslSocket, parameters);
    }
  }
```
这两个问题归根到底都是替换了Host所造成的。所以我们按照以上思路解决了！

# 使用
- Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
- Add the dependency
```
dependencies {
    implementation 'com.github.Jack6Wolf:OkHttps:1.0.0'
}
```

- Each request must be preceded by a request header
```
builder.addHeader("host", "xxx")
```