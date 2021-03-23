package com.star.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author jack
 * @since 2021/3/16 16:12
 */
public class NetworkUtils {
    /**
     * 14      * 校验是否可以连通
     * 15      * @param ip
     * 16      * @return true/false
     * 17
     */
    public static boolean isConnect() {
        boolean connect = false;
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            //百度的ip
            process = runtime.exec("ping 39.156.66.18");
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            isr.close();
            br.close();
            if (null != sb && !sb.toString().equals("")) {
                String logString = "";
                if (sb.toString().indexOf("TTL") > 0) {
                    // 网络畅通
                    connect = true;
                } else {
                    // 网络不畅通
                    connect = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connect;
    }
}
