package com.example.rth.serviecs;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

/**
 * Created by rth on 15-10-4.
 * 播放音乐的服务
 */
public class MusicServices extends Service implements Handler.Callback{

    private MediaPlayer player; //音乐播放器
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

    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
        mHan = new Handler(Looper.myLooper(),this);
        //开启工作线程
        backWork = new HandlerThread("music_work");
        backWork.start();
        workHan = new Handler(backWork.getLooper(),this);
    }

    /**
     * 初始化Mediaplayer
     */
    private void initMediaPlayer() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnBufferingUpdateListener(bufferingUpdateListener);
        player.setOnPreparedListener(preparedListener);
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
     * @param url 网络音乐地址
     */
    public void requirePlay(String url) {
        workHan.obtainMessage(REQUERY_PLAY,-1,-1,url).sendToTarget();
    }

    /**
     * 播放网络音乐
     * @param url 网络音乐的链接
     */
    private void playUrl(String url) {
        try {
            mHan.obtainMessage(LOAD_MUSIC,-1,-1).sendToTarget();
            player.reset();
            player.setDataSource(url);
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
    /**
     * 接收Handler发送的消息并处理
     * @param message
     * @return
     */
    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case REQUERY_PLAY:
                playUrl((String)message.obj);
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
        }
        return false;
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
    }
}
