package com.example.rth.serviecs;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.rth.util.MusicData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by rth on 15-10-4.
 * 播放音乐的服务
 */
public class MusicServices extends Service implements Handler.Callback{

    private MediaPlayer player; //音乐播放器
    //记录要播放的音乐信息
    private Map<String,MusicData> musicDatas = new HashMap<>();
    //测试用的音乐信息
    private Map<String,String> testMusicDatas = new HashMap<>();
    //volley的网络请求队列
    private RequestQueue queue;
    //网络流媒体的缓冲监听
    private final MediaPlayer.OnBufferingUpdateListener bufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

        }
    };
    //缓冲监听器
    private final MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            mHan.obtainMessage(LOADED_MUSIC,-1,-1).sendToTarget();
        }
    };
    //错误监听器
    private final MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            return false;
        }
    };

    //返回MusicService实例
    public class LocalMusicBinder extends Binder {
        public MusicServices getService() {
            return MusicServices.this;
        }
    }
    private LocalMusicBinder localMusicBinder = new LocalMusicBinder();

    //回调接口
    private CallbackInMusicService callbackInMusicService;
    //接收异步消息在主线程处理
    private static Handler mHan;
    //接收异步在工作线程处理
    private HandlerThread backWork;
    private static Handler workHan;
    //后台接口
    private static final String API_URL = "http://1.stepmusics.sinaapp.com/App/Api/getplaydata.php?ssid=";
    private MusicData playData; //正在播放的音乐信息
    //assets资源管理器
    private AssetManager assetManager;

    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
        mHan = new Handler(Looper.myLooper(),this);
        queue = Volley.newRequestQueue(this);
        //开启工作线程
        backWork = new HandlerThread("music_work");
        backWork.start();
        workHan = new Handler(backWork.getLooper(),this);
        assetManager = getAssets();
    }

    /**
     * 初始化Mediaplayer
     */
    private void initMediaPlayer() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnBufferingUpdateListener(bufferingUpdateListener);
        player.setOnPreparedListener(preparedListener);
        //测试添加一些音乐
        testMusicDatas.put("Nidhogg", "Strip It Down.m4a");
        testMusicDatas.put("20.0", "Strip It Down.m4a");
        testMusicDatas.put("720", "What Do You Mean_.m4a");
        testMusicDatas.put("30.0","What Do You Mean_.m4a");
        testMusicDatas.put("50.0", "Do not Speak.m4a");

    }

    /**
     * 设置回调接口
     * @param callbackInMusicService
     */
    public void setCallbackInMusicService(CallbackInMusicService callbackInMusicService) {
        this.callbackInMusicService = callbackInMusicService;
    }

    /**
     * 播放音乐
     */
    public void play() {
        if(player != null) player.start();
    }

    /**
     * 暂停
     */
    public void pause() {
        if(player != null) player.pause();
    }

    /**
     * 停止播放音乐
     */
    public void stop() {
        if(player != null && player.isPlaying()) {
            player.stop();
            player.release();
        }
    }

    /**
     * 请求播放网络音乐
     * @param key 目标音乐的键值
     */
    public void requirePlay(String key) {
        workHan.obtainMessage(REQUERY_PLAY,-1,-1,key).sendToTarget();
    }

    /**
     * 获取要播放音乐的链接
     * @param ssid  当前链接的wifi名称
     */
    private void getPlayUrl(String ssid) {

        String playUrl = null;
        MusicData data = null;
        try {
            data = musicDatas.get(ssid);
        }catch (Exception e) {
            data = null;
        }
        if (data != null) {
            //本地有缓存
            playData = data;
            playUrl(data.musicUrl);
        }else {
            Log.e("开始获取热点信息", ssid);
            //从网络加载
            StringRequest request = new StringRequest(Request.Method.GET, API_URL + ssid, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mHan.obtainMessage(LOADED_DATA,-1,-1).sendToTarget();
                    if(!TextUtils.isEmpty(response)) {
                        try {
                            JSONObject message = new JSONObject(response);
                            MusicData musicData = new MusicData();
                            musicData.wifiName = message.getString("name");
                            musicData.range = message.getInt("range");
                            musicData.musicUrl = message.getString("music_url");
                            musicData.des = message.getString("description");
                            musicDatas.put(musicData.wifiName,musicData);
                            //播放音乐
                            playData = musicData;
                            playUrl(musicData.musicUrl);
                        } catch (JSONException e) {
                            mHan.obtainMessage(LOAD_FAILED,-1,-1).sendToTarget();
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("getPlayUrl","volley 加载出错"+error.getMessage());
                    mHan.obtainMessage(LOAD_FAILED,-1,-1).sendToTarget();
                }
            });
            mHan.obtainMessage(LOADING_DATA,-1,-1).sendToTarget();
            queue.add(request);
        }
    }

    /**
     * 播放网络音乐
     * @param url 待播放音乐的链接
     */
    private void playUrl(String url) {
        Log.e("url is ",url);
        try {
            mHan.obtainMessage(LOAD_MUSIC,-1,-1).sendToTarget();
            player.reset();
            player.setDataSource(url);
            player.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放本地资源里的音乐
     * @param afd
     */
    private void playUrl(AssetFileDescriptor afd) {
        try {
            mHan.obtainMessage(LOAD_MUSIC,-1,-1).sendToTarget();
            player.reset();
            player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),
                    afd.getLength());
            player.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //关闭后台线程
        if(Build.VERSION.SDK_INT >= 18) {
            backWork.quitSafely();
        }else {
            backWork.stop();
        }
        localMusicBinder = null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        workHan.obtainMessage(STOP_PLAY,-1,-1).sendToTarget();
        return super.onUnbind(intent);
    }

    private static final int REQUERY_PLAY =0;  //请求播放网络音乐
    private static final int LOAD_MUSIC = 1;    //正在加载网络音乐
    private static final int LOADED_MUSIC = 2;    //网络音乐已经加载
    private static final int START_PLAY = 3;    //开始播放网络音乐
    private static final int STOP_PLAY = 4; //停止播放音乐
    private static final int LOADING_DATA = 5; //正在加载热点信息
    private static final int LOADED_DATA = 6; //热点信息加载完毕
    private static final int LOAD_FAILED = 7; //获取热点信息失败
    /**
     * 接收Handler发送的消息并处理
     * @param message
     * @return
     */
    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case REQUERY_PLAY:
                playLcalMusic((String) message.obj);
                //getPlayUrl((String) message.obj);
                return true;
            case LOAD_MUSIC:
                if(callbackInMusicService != null) callbackInMusicService.preparingMusic();
                return true;
            case LOADED_MUSIC:
                if(callbackInMusicService != null) callbackInMusicService.startPlay();
                workHan.obtainMessage(START_PLAY,-1,-1).sendToTarget();
                return true;
            case START_PLAY:
                play();
                return true;
            case STOP_PLAY:
                stop();
                return true;
            case LOADING_DATA:
                if (callbackInMusicService != null) callbackInMusicService.loadingData(0);
                return true;
            case LOADED_DATA:
                if (callbackInMusicService != null) callbackInMusicService.loadingData(1);
                return true;
            case LOAD_FAILED:
                if (callbackInMusicService != null) callbackInMusicService.loadingData(2);
                return true;
        }
        return false;
    }

    /**
     * 播放本地存储的音乐
     * @param obj 目标音乐对应的键值
     */
    private void playLcalMusic(String obj) {
        //随机生成一个音乐
        Random random = new Random();
        int in = random.nextInt(4);
        switch (in) {
            case 0:
                obj = "Nidhogg";
                break;
            case 1:
                obj = "20.0";
                break;
            case 2:
                obj = "720";
                break;
            case 3:
                obj = "30.0";
                break;
            default:
                obj = "50.0";
                break;
        }
        String musicName = testMusicDatas.get(obj);
        if(TextUtils.isEmpty(musicName)) {
            return;
        }
        try {
            AssetFileDescriptor afd = assetManager.openFd(musicName);
            playUrl(afd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localMusicBinder;
    }

    /**
     * 回调接口
     */
    public interface CallbackInMusicService {

        /**
         * 正在加载网络音乐
         */
        void preparingMusic();

        /**
         * 开始播放音乐
         */
        void startPlay();

        /**
         * 正在加载热点信息
         * @param flag    0表示开始加载,1表示加载完毕,2表示加载信息失败
         */
        void loadingData(int flag);
    }
}
