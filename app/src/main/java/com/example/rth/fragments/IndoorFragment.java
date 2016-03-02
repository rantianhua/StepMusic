package com.example.rth.fragments;

import android.app.AlertDialog;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rth.adapter.WifiListAdapter;
import com.example.rth.base.BaseFragment;
import com.example.rth.base.OnRecylerViewItemClickListener;
import com.example.rth.stepmusic.R;
import com.example.rth.util.MyWifiManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rth on 16-2-21.
 */
public class IndoorFragment extends BaseFragment implements
        OnRecylerViewItemClickListener{

    //RecylerView的Adapter
    private WifiListAdapter adapter;
    //显示连接的wifi的名称
    private TextView tvWifiName;
    //显示wifi的ssid值
    private TextView tvWifiSsid;
    //SwipeRefreshLayout
    private SwipeRefreshLayout srf;
    //管理wifi
    private MyWifiManager myWifiManager;
    //标识wifi是否已经打开
    private boolean wifiOpened = false;
    //接收异步消息
    private static final Handler mainHan = new Handler(Looper.myLooper());
    //wifi密码
    private String wifiPass;
    //wifi加密类型
    private int wifiPassType;
    //要连接的wifi
    private ScanResult connScannResult;
    //获取wifi密码的弹出框
    private AlertDialog passAlert;
    //自定义的弹出框view
    private View cusAlertView;
    //标识要不要连接新的wifi
    private boolean reconnect = true;
    //MywifiManager的回调接口
    private final MyWifiManager.CallBack wifiCallback = new MyWifiManager.CallBack() {
        @Override
        public void wifiEnabled() {
            wifiOpened = true;
        }

        @Override
        public void rssiChanged(int rssi) {
            //rssi值发生变化
            tvWifiSsid.setText(getString(R.string.current_strength, rssi));
            if(rssi <= 3) {
                //换其他的wifi去连接,先重新搜索一便
                if(reconnect) {
                    searchWifi();
                }
            }
        }

        @Override
        public void wifiChanged(String ssid) {
            if(ssid.contains("unknown")) {
                tvWifiName.setText(getString(R.string.current_wifi,"正在连接..."));
            }else {
                reconnect = true;
                tvWifiName.setText(getString(R.string.current_wifi,ssid));
                playMusic(ssid);
            }
        }
    };
    //搜索到的wifi结果
    private List<ScanResult> results;
    //回调
    private CallbackInIndoorFrag callback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new WifiListAdapter(getContext(),this);
        myWifiManager = new MyWifiManager(getContext());
        myWifiManager.setCallBack(wifiCallback);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_indoor,container,false);
        setupView(view);
        bindsEvents();
        return view;
    }

    @Override
    public void setupView(View view) {
        tvWifiName = (TextView) view.findViewById(R.id.tv_current_wifi);
        tvWifiSsid = (TextView) view.findViewById(R.id.tv_current_strength);
        tvWifiName.setText(getString(R.string.current_wifi,"无"));
        tvWifiSsid.setText(getString(R.string.current_strength,0));

        srf = (SwipeRefreshLayout) view.findViewById(R.id.frag_indoor_srf);
        srf.setColorSchemeColors(getResources().getColor(R.color.pink_500));
        //禁止下拉刷新
        srf.setEnabled(false);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.frag_indoor_recyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        passAlert = new AlertDialog.Builder(getContext()).create();
        cusAlertView = LayoutInflater.from(getContext()).inflate(R.layout.pass_dialog,null);
        passAlert.setView(cusAlertView);
    }

    @Override
    public void bindsEvents() {
        super.bindsEvents();
        //取消连接wifi
        ((TextView)cusAlertView.findViewById(R.id.pass_dialog_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiPass = null;
                ((EditText) cusAlertView.findViewById(R.id.pass_dialog_et_pass)).setText("");
                passAlert.dismiss();
            }
        });
        //取得wifi密码并连接
        ((TextView)cusAlertView.findViewById(R.id.pass_dialog_sure)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiPass = ((EditText) cusAlertView.findViewById(R.id.pass_dialog_et_pass)).getText().toString();
                passAlert.dismiss();
                myWifiManager.connectWifi(connScannResult, wifiPass, wifiPassType);
                ((EditText) cusAlertView.findViewById(R.id.pass_dialog_et_pass)).setText("");
            }
        });
    }

    public void setCallback(CallbackInIndoorFrag callback) {
        this.callback = callback;
    }

    @Override
    public void onStart() {
        super.onStart();
        wifiOpened = myWifiManager.isWifiEnable();
        if(!wifiOpened) {
            //开启wifi
            myWifiManager.startWifi();
        }else {
            String apName = myWifiManager.getConnectWifiName();
            tvWifiName.setText(getString(R.string.current_wifi, TextUtils.isEmpty(apName) ? "未知热点" : apName));
            if(callback != null && !callback.isPlaying()) {
                callback.playMusic(apName);
            }
        }
    }

    /**
     * 根据wifi名称播放不同音乐
     * @param ssid 当前连接的wifi热点名称
     */
    private void playMusic(String ssid) {
        if(callback != null) {
            callback.playMusic(ssid);
        }
    }

    /**
     * 搜索ｗｉｆｉ
     */
    public void refreshWifi() {
        srf.setRefreshing(true);
        searchWifi();
    }

    @Override
    public void onItemClicked(Object... ots) {
        int position = (int)ots[0];
        //连接制定wifi
        ScanResult wifi = results.get(position);
        connectWifi(wifi);
    }

    /**
     * 连接指定的wifi
     * @param wifi 要连接的wifi
     */
    private void connectWifi(ScanResult wifi) {
        if(TextUtils.isEmpty(wifi.SSID)) {
            Toast.makeText(getContext(), "该wifi被隐藏了", Toast.LENGTH_SHORT).show();
            return;
        }
        if(myWifiManager.getConnectWifiName().equals(wifi.SSID)) {
            Toast.makeText(getContext(),wifi.SSID + "已连接",Toast.LENGTH_SHORT).show();
            return;
        }
        //检查该wifi有没有配置过
        WifiConfiguration configuration = myWifiManager.haveConfiguration(wifi);
        if(configuration != null) {
            //已经配置过了,直接连接
            myWifiManager.connectWifi(configuration);
            return;
        }
        //先检查加密类型类型
        String keyMethod = wifi.capabilities;
        if(keyMethod.contains("WPA") || keyMethod.contains("wpa")) {
            wifiPassType = MyWifiManager.TYPE_WPA;
        }else if(keyMethod.contains("WEP") || keyMethod.contains("wep")) {
            wifiPassType = MyWifiManager.TYPE_WEP;
        }else {
            wifiPassType = MyWifiManager.TYPE_NONE;
        }
        if(wifiPassType != MyWifiManager.TYPE_NONE) {
            //获取密码
            connScannResult = wifi;
            ((TextView)cusAlertView.findViewById(R.id.pass_dialog_title)).setText(wifi.SSID);
            passAlert.show();
        }else {
            myWifiManager.connectWifi(wifi, null, wifiPassType);
        }
    }

    /**
     * 搜索周围热点
     */
    private void searchWifi() {
        if (wifiOpened) {
            srf.setRefreshing(true);
            myWifiManager.startScan();
            mainHan.postDelayed(new Runnable() {
                @Override
                public void run() {
                    catchScanResults();
                }
            }, 1000);
        } else {
            Toast.makeText(getContext(), "wifi正在开启,请稍后", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取扫描结果
     */
    private void catchScanResults() {
        if (srf.isRefreshing()) {
            srf.setRefreshing(false);
        }
        //获取搜索结果
        results = myWifiManager.getScanResults();
        //更新RecylerView的数据
        if (results.size() > 0) {
            adapter.clearCurrentData();
            for (int i = 0; i < results.size(); i++) {
                Map<String, String> data = new HashMap<String, String>();
                String ssid = results.get(i).SSID;
                data.put("name", TextUtils.isEmpty(ssid) ? "unknown name" : ssid);
                adapter.addWifiAp(data);
            }
            adapter.notifyDataSetChanged();
            //检查是否需要自动重新连接
            if (!results.get(0).SSID.equals(myWifiManager.getConnectWifiName()) && reconnect) {
                reconnect = false;
                //连接当前信号最强的一个ap
                connectWifi(results.get(0));
            }
        } else {
            Toast.makeText(getContext(), "未搜索到任何wifi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        //取消广播
        myWifiManager.unBindBroadcast();
        super.onDestroy();
        setCallback(null);
    }

    /**
     * 回调接口，和Activity通信
     */
    public interface CallbackInIndoorFrag {

        /**
         * 请求播放音乐
         * @param ssid 当前连接的wifi
         */
        void playMusic(String ssid);

        /**
         * 判断有没有在播放音乐
         * @return
         */
        boolean isPlaying();

    }
}
