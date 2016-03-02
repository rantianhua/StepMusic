package com.example.rth.util;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rth on 15-10-5.
 */
public class Constants {

    //杀破狼的链接
    public static final String shaPoLang = "http://stepmusics-library.stor.sinaapp.com/JS%20-%20%E6%9D%80%E7%A0%B4%E7%8B%BC%20%5Bmqms2%5D.mp3";
    //清风徐来的链接
    public static final String wind_url = "http://stepmusics-library.stor.sinaapp.com/%E6%B8%85%E9%A3%8E%E5%BE%90%E6%9D%A5.mp3";

    //存储一些颜色值用来填充圆环
    public static final Map<Integer,Integer> CIRCLE_COLORS = new HashMap<>();
    static {
        CIRCLE_COLORS.put(0, Color.argb(80,255,0,0));
        CIRCLE_COLORS.put(1, Color.argb(80,0,255,0));
        CIRCLE_COLORS.put(2, Color.argb(80,0,0,255));
    }
}
