package com.example.rth.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by rantianhua on 15-5-26.
 * wifi监听广播
 */
public class WiFiBroadcastReceiver extends BroadcastReceiver{

    //管理wifi连接的服务
    private MyWifiManager myWifiManager;

    public WiFiBroadcastReceiver(MyWifiManager manager) {
        super();
        this.myWifiManager = manager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiManager.WIFI_STATE_ENABLED) {
                //wifi开启
                myWifiManager.wifiEnabled();
            }else if(state == WifiManager.WIFI_STATE_DISABLED) {
                //wifi关闭
                myWifiManager.wifiDisable();
            }
        }else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            WifiInfo info = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if(info != null) {
                //连接了新的wifi
                myWifiManager.wifiConnected(info.getSSID());
            }
        }else if(WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            //rssi值发生生变化
            int newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI,0);
            //计算信号强度
            int level = WifiManager.calculateSignalLevel(newRssi,5);
            myWifiManager.rssiChanged(level+1);
        }
    }
}
