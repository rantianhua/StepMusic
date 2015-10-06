package com.example.rth.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by rth on 15-10-6.
 * 管理wifi的搜索和连接
 */
public class MyWifiManager {

    private final String TAG = getClass().getSimpleName();
    //wifi的热点名
    private String SSID;
    //wifi管理者
    private WifiManager manager;
    //wifi状态的广播接收器
    private BroadcastReceiver wifiReceiver;
    private CallBack callback;  //回调接口
    private Context context;

    public MyWifiManager(Context context) {
        this.context = context;
        manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WiFiBroadcastReceiver(this);
        context.registerReceiver(wifiReceiver,getIntentFilter());
    }

    /**
     * 比较两个热点名是否相同
     * @param ssid1
     * @param ssid2
     * @return
     */
    public static boolean compareTwoSsid(String ssid1,String ssid2) {
        //有的设备有双引号
        ssid1 = ssid1.replaceAll("\"","");
        ssid2 = ssid2.replaceAll("\"", "");
        if(ssid1.equalsIgnoreCase(ssid2)) {
            return true;
        }
        return false;
    }

    //检查热点是否已经开启
    public boolean isWifiApEnabled() {
        try {
            Method method = manager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(manager);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //获取当前热点的名称
    public String getConnectWifiName() {
        WifiInfo info = manager.getConnectionInfo();
        return info == null ? "为连接" : info.getSSID();
    }

    /**
     * 获取当前连接的rssi值
     * @return
     */
    public int getConnectionRssi() {
        WifiInfo info = manager.getConnectionInfo();
        int rssi = info == null ? 0 : info.getRssi();
        return rssi;
    }

    //开启wifi
    public void startWifi() {
        //如果热点是开着的，先关闭热点
        if(isWifiApEnabled()) {
            closeWifiAp();
            try {
                Thread.sleep(50);
            }catch (Exception e) {

            }
        }
        manager.setWifiEnabled(true);
    }

    public void closeWifi() {
        if(manager.isWifiEnabled()) {
            manager.setWifiEnabled(false);
        }
    }

    //设置接口
    public void setCallBack(CallBack callback) {
        this.callback = callback;
    }

    //检查wifi是否已开启
    public boolean isWifiEnable() {
        return manager.isWifiEnabled();
    }

    //wifi已经开启
    public void wifiEnabled() {
        if(callback != null) {
            //通知接口调用方
            callback.wifiEnabled();
        }
    }

    /**
     * wifi关闭了
     */
    public void wifiDisable() {
    }

    /**
     * wifi已经连接
     * @param ssid
     */
    public void wifiConnected(String ssid) {
        if(callback != null) {
            // callback.wifiApConnected(ssid);
        }
    }

    /**
     * 连接wifi热点
     * @param ssid 热点名称
     * @return
     */
    public boolean connectAction(String ssid,String pwd) {
        WifiConfiguration configuration = null;
        //检查是否已经有连接过该热点
        WifiConfiguration tempConfig = isExist(SSID);
        if(tempConfig != null) {
            //连接过，直接连接
            Log.e("connectAction", "之前连接过");
            configuration = tempConfig;
        }else {
            configuration = setUpWifiConfig(ssid,pwd);
        }
        if(configuration == null) {
            return false;
        }
        //检查是否已有wifi连接上，有则断开连接，
        isWifiConneted(true);
        int netId = manager.addNetwork(configuration);
        manager.enableNetwork(netId, true);
        manager.reassociate();
        return true;
    }

    /**
     *
     * @param ssid 要连接的wifi热点的名称
     * @return true 如果检查出该热点已经连接上了
     */
    public boolean ssidConnected(String ssid) {
        String connectedSSID = isWifiConneted(false);
        if(!TextUtils.isEmpty(connectedSSID)) {
            return compareTwoSsid(ssid,connectedSSID);
        }
        return false;
    }

    /**
     * 检查wifi是否已有连接
     * @param disConnect    //是否断开该连接
     * @return
     */
    private String isWifiConneted(boolean disConnect) {
        WifiInfo info = manager.getConnectionInfo();
        if(info != null) {
            String ssid = info.getSSID();
            if(disConnect) {
                //关闭该连接
                List<WifiConfiguration> configurations = manager.getConfiguredNetworks();
                for(WifiConfiguration con : configurations) {
                    if(compareTwoSsid(ssid,con.SSID)) {
                        //关闭该wifi
                        int netId = con.networkId;
                        manager.disableNetwork(netId);
                        manager.disconnect();
                        break;
                    }
                }
                return null;
            }else {
                return ssid;
            }
        }else {
            return null;
        }
    }

    /**
     * @param ssid 热点名称
     * @return 存在的热点配置
     */
    private WifiConfiguration isExist(String ssid) {
        List<WifiConfiguration> existingConfigs = manager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (compareTwoSsid(existingConfig.SSID,ssid)) {
                return existingConfig;
            }
        }
        return null;
    }

    /**
     * @param ssid  the wifiAp's name
     * @return WifiConfiguration
     */
    private WifiConfiguration setUpWifiConfig(String ssid,String pwd) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.allowedAuthAlgorithms.clear();
        configuration.allowedGroupCiphers.clear();
        configuration.allowedKeyManagement.clear();
        configuration.allowedPairwiseCiphers.clear();
        configuration.allowedProtocols.clear();
        configuration.SSID = "\"" + ssid + "\"";

        configuration.preSharedKey = "\"" + pwd + "\"";
        configuration.hiddenSSID = true;
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        return configuration;
    }

    /**
     * statrt scan wifi device
     */
    public void startScan() {
        manager.startScan();
    }

    public List<ScanResult> getScanResults() {
        List<ScanResult> results = manager.getScanResults();
        //按强度排序
        for(int i=0;i<results.size();i++)
            for(int j=1;j<results.size();j++)
            {
                if(results.get(i).level<results.get(j).level)    //level属性即为强度
                {
                    ScanResult temp = null;
                    temp = results.get(i);
                    results.set(i, results.get(j));
                    results.set(j, temp);
                }
            }
        return results;
    }

    /**
     * 取消广播
     */
    public void unBindBroadcast() {
        context.unregisterReceiver(wifiReceiver);
    }

    private IntentFilter getIntentFilter() {
        //create an intentfilter for broadcast receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        return intentFilter;
    }

    //关闭wifi热点
    public void closeWifiAp() {
        try {
            Method method = manager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(manager);
            Method method2 = manager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            method2.invoke(manager, config, false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 信号强度发生变化
     * @param level
     */
    public void rssiChanged(int level) {
        if(callback == null) return;
        callback.rssiChanged(level);
    }

    public interface CallBack {

        /**
         * wifi已经打开
         */
        void wifiEnabled();

        /**
         * 连接的rssi发生变化
         * @param rssi
         */
        void rssiChanged(int rssi);
    }

}
