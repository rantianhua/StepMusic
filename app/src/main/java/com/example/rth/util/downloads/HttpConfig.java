package com.example.rth.util.downloads;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by rth on 15-10-5.
 * 配置Http访问的header以及超时信息
 */
public class HttpConfig {

    /**
     * 获取HttpURLConnection访问网络资源
     * @param url   资源的url
     * @return
     */
    public static HttpURLConnection getHttpConnection(URL url) {
        try {
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            //设置请求的超时时间
            http.setConnectTimeout(5000);
            //请求方式
            http.setRequestMethod("GET");
            //浏览器可接受的MIME类型
            http.setRequestProperty(
                    "Accept",
                    "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            // 浏览器所希望的语言种类，当服务器能够提供一种以上的语言版本时要用到
            http.setRequestProperty("Accept-Language", "zh-CN");
            // 包含一个URL，用户从该URL代表的页面出发访问当前请求的页面。
            http.setRequestProperty("Referer", url.toString());
            // 设置字符集
            http.setRequestProperty("Charset", "UTF-8");
            //浏览器类型
            http.setRequestProperty(
                    "User-Agent",
                    "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
            //设置为持久连接
            http.setRequestProperty("Connection", "Keep-Alive");
            return http;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
