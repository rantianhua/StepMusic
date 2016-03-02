package com.example.rth.stepmusic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rth.serviecs.MusicServices;
import com.example.rth.util.MyWifiManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity implements ServiceConnection,MusicServices.CallbackInMusicService{

    private MusicServices musicServices;    //播放音乐的服务
    private MyWifiManager myWifiManager;  //管理wifi
    private ProgressDialog pd;
    private TextView tvWifiName;    //显示连接的wifi的名称
    private TextView tvWifiSsid;    //显示wifi的ssid值
    private Button btnSearchWifi;   //搜索wifi的按钮
    private ListView listView;  //显示搜索到的wifi
    private ProgressBar progressBar;    //显示正在搜索
    private SimpleAdapter adapter;  //listView适配器
    private boolean wifiOpened = false; //标识wifi是否已经打开

    private static final Handler mainHan = new Handler(Looper.myLooper());  //接收异步消息
    private List<Map<String,String>> listData = new ArrayList<>();  //listView的数据源

    private String wifiPass;   //wifi密码
    private int wifiPassType;      //wifi加密类型
    private ScanResult connScannResult; //要连接的wifi
    private AlertDialog passAlert;  //获取wifi密码的弹出框
    private View cusAlertView;  //自定义的弹出框view
    private boolean reconnect = true;  //标识要不要连接新的wifi
    private boolean needPlayMusic = false;  //标识需不需要播放音乐
    private String playSSid; //需要播放音乐的wifi
    private boolean isPlaying = false;  //标识是否正在播放音乐

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

    private List<ScanResult> results;   //搜索到的wifi结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //绑定服务
        bindService(new Intent(this, MusicServices.class), this, BIND_AUTO_CREATE);
    }

    private void initView() {
        pd = new ProgressDialog(this);
        tvWifiName = (TextView) findViewById(R.id.tv_current_wifi);
        tvWifiSsid = (TextView) findViewById(R.id.tv_current_strength);
        listView = (ListView) findViewById(R.id.listview);
        btnSearchWifi = (Button) findViewById(R.id.btn_search_wifi);
        progressBar = (ProgressBar) findViewById(R.id.pb_loading);
        passAlert = new AlertDialog.Builder(this).create();
        cusAlertView = LayoutInflater.from(this).inflate(R.layout.pass_dialog,null);
        passAlert.setView(cusAlertView);
        initEvent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(myWifiManager == null) {
            myWifiManager = new MyWifiManager(this);
            myWifiManager.setCallBack(wifiCallback);
        }
        wifiOpened = myWifiManager.isWifiEnable();
        if(!wifiOpened) {
            //开启wifi
            myWifiManager.startWifi();
        }else {
            String apName = myWifiManager.getConnectWifiName();
            tvWifiName.setText(getString(R.string.current_wifi, TextUtils.isEmpty(apName) ? "未知热点" : apName));
            if(!isPlaying) {
                playMusic(apName);
            }
        }
    }

    /**
     * 根据wifi名称播放不同音乐
     * @param ssid
     */
    private void playMusic(String ssid) {
        playSSid = ssid;
        if(musicServices != null) {
            musicServices.requirePlay(playSSid);
        }else {
            needPlayMusic = true;
        }
    }

    /**
     * 设置事件监听
     */
    private void initEvent() {
        btnSearchWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchWifi();
            }
        });
        //listview的点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //连接制定wifi
                ScanResult wifi = results.get(i);
                connectWifi(wifi);
            }
        });
        //取消连接wifi
        ((TextView)cusAlertView.findViewById(R.id.pass_dialog_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiPass = null;
                passAlert.dismiss();
            }
        });
        //取得wifi密码并连接
        ((TextView)cusAlertView.findViewById(R.id.pass_dialog_sure)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiPass = ((EditText)cusAlertView.findViewById(R.id.pass_dialog_et_pass)).getText().toString();
                passAlert.dismiss();
                myWifiManager.connectWifi(connScannResult,wifiPass,wifiPassType);
            }
        });
    }


    /**
     * 连接指定的wifi
     * @param wifi 要连接的wifi
     */
    private void connectWifi(ScanResult wifi) {
        if(TextUtils.isEmpty(wifi.SSID)) {
            Toast.makeText(this,"该wifi被隐藏了",Toast.LENGTH_SHORT).show();
            return;
        }
        if(myWifiManager.getConnectWifiName().equals(wifi.SSID)) {
            Toast.makeText(this,wifi.SSID + "已连接",Toast.LENGTH_SHORT).show();
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
            progressBar.setVisibility(View.VISIBLE);
            myWifiManager.startScan();
            mainHan.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                    //获取搜索结果
                    results = myWifiManager.getScanResults();
                    //更新listView的数据
                    if (results.size() > 1) {
                        listData.clear();
                        for (int i = 0; i < results.size(); i++) {
                            Map<String, String> data = new HashMap<String, String>();
                            String ssid = results.get(i).SSID;
                            data.put("name", TextUtils.isEmpty(ssid) ? "unknown name" : ssid);
                            listData.add(data);
                        }
                        if (adapter == null) {
                            adapter = new SimpleAdapter(MainActivity.this, listData, android.R.layout.simple_list_item_1,
                                    new String[]{"name"}, new int[]{android.R.id.text1});
                            listView.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        if (!results.get(0).SSID.equals(myWifiManager.getConnectWifiName()) && reconnect) {
                            reconnect = false;
                            connectWifi(results.get(0));
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "未搜索到任何wifi", Toast.LENGTH_SHORT).show();
                    }
                }
            }, 1000);
        } else {
            Toast.makeText(MainActivity.this, "wifi正在开启,请稍后", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        musicServices = ((MusicServices.LocalMusicBinder)iBinder).getService();
        musicServices.setCallbackInMusicService(this);
        if(needPlayMusic) {
            needPlayMusic = false;
            musicServices.requirePlay(playSSid);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicServices.setCallbackInMusicService(null);
        musicServices = null;
    }

    @Override
    protected void onDestroy() {
        //取消广播
        myWifiManager.unBindBroadcast();
        super.onDestroy();
        unbindService(this);
    }

    /**
     * 在MusicService中回调
     * 正在加载网络音乐
     */
    @Override
    public void preparingMusic() {
        pd.setMessage("正在加载新的音乐...");
        pd.show();
    }

    /**
     * 在MusicService中回调
     * 开始播放网络音乐
     */
    @Override
    public void startPlay() {
        pd.dismiss();
        isPlaying = true;
    }

    /**
     * 在MusicService中回调
     * @param flag    0表示开始加载,1表示加载完毕,2表示加载信息失败
     */
    @Override
    public void loadingData(int flag) {
        if(flag == 0) {
            pd.setMessage("正在加载热点资源...");
            pd.show();
        }else if(flag == 1) {
            if(pd.isShowing()) {
                pd.dismiss();
            }
        }else if(flag == 2) {
            if(pd.isShowing()) {
                pd.dismiss();
            }
            Toast.makeText(MainActivity.this, "未获取到热点信息", Toast.LENGTH_SHORT).show();
        }
    }
}
