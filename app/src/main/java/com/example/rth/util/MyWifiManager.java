package com.example.rth.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
        String ssid = "未连接";
        if(info != null) {
            ssid = info.getSSID();
            ssid = cutQuotations(ssid);
        }
        return ssid;
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
            ssid = cutQuotations(ssid);
            callback.wifiChanged(ssid);
        }
    }

    /**
     * 检查wifi是否已有连接,有则断开连接
     * @return
     */
    private void disconnectWifi() {
        WifiInfo info = manager.getConnectionInfo();
        if(info != null) {
            //关闭该连接
            String ssid = info.getSSID();
            List<WifiConfiguration> configurations = manager.getConfiguredNetworks();
            for(WifiConfiguration con : configurations) {
                if(ssid.equals(con.SSID)) {
                    //关闭该wifi
                    int netId = con.networkId;
                    manager.disableNetwork(netId);
                    manager.disconnect();
                    break;
                }
            }
        }
    }

    /**
     * 扫描周围wifi
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
     * 连接制定的wifi
     * @param wifi 要连接的wifi
     * @param pass  wifi密码
     * @param type wifi加密类型
     */
    public void connectWifi(ScanResult wifi,String pass,int type) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.allowedAuthAlgorithms.clear();
        configuration.allowedGroupCiphers.clear();
        configuration.allowedKeyManagement.clear();
        configuration.allowedPairwiseCiphers.clear();
        configuration.allowedProtocols.clear();
        configuration.SSID = wifi.SSID;
        switch (type) {
            case TYPE_NONE:
                configuration.wepKeys[0] = "";
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                configuration.wepTxKeyIndex = 0;
                break;
            case TYPE_WEP:
                configuration.hiddenSSID = true;
                configuration.wepKeys[0] = "\"" + pass + "\"";
                configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                configuration.wepTxKeyIndex = 0;
                break;
            case TYPE_WPA:
                configuration.preSharedKey = "\""+pass+"\"";
                configuration.hiddenSSID = true;
                configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                configuration.status = WifiConfiguration.Status.ENABLED;
                break;
        }
        connectWifi(configuration);
    }

    /**
     * 连接指定wifi,重载与{@connectWifi(ScanResult wifi,String pass,int type)}
     * @param configuration
     */
    public void connectWifi(WifiConfiguration configuration) {
        //检查是否已有wifi连接上，有则断开连接，
        disconnectWifi();
        int netId = manager.addNetwork(configuration);
        manager.enableNetwork(netId, true);
    }

    /**
     * 判断有没有配置过该连接
     * @param wifi 待判断的wifi
     * @return 配置过的Wificonfiguration,没有返回null
     */
    public WifiConfiguration haveConfiguration(ScanResult wifi) {
        List<WifiConfiguration> configurations = manager.getConfiguredNetworks();
        if(configurations != null) {
            for (WifiConfiguration configuration : configurations) {
                if(cutQuotations(configuration.SSID).equals(wifi.SSID)) {
                    return configuration;
                }
            }
        }
        return null;
    }

    /**
     * 去除ssid多余的双引号,便于比对
     * @param ssid
     * @return 去掉多余双引号后的ssid
     */
    private String cutQuotations(String ssid) {
        if(ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1);
            ssid = ssid.substring(0,ssid.length()-1);
        }
        return ssid;
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

    //wifi加密类型
    public static final int TYPE_NONE = 0;  //没有密码的类型
    public static final int TYPE_WPA = 1;  //wpa加密
    public static final int TYPE_WEP = 2;  //wep加密


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

        /**
         * 连接的wifi改变了
         * @param ssid
         */
        void wifiChanged(String ssid);
    }

}
